import queue
import threading
import json
from typing import Callable, Any

event_queue: queue.Queue = queue.Queue()

_lock: threading.Lock = threading.Lock()
_subscribers: list[Callable | None] = []


def broadcast_event(event_type: str, data: dict | None = None) -> None:
    """Push an event to the shared queue for all connected Bluetooth clients."""
    evt: dict[str, Any] = {"event": event_type, "data": data or {}}
    event_queue.put(evt)
    _notify_subscribers(evt)


def subscribe(callback: Callable) -> int:
    """Register a callback to be called for every event (used by web UI, etc)."""
    with _lock:
        _subscribers.append(callback)
        return len(_subscribers) - 1


def unsubscribe(index: int) -> None:
    """Remove a subscriber callback by index."""
    with _lock:
        if 0 <= index < len(_subscribers):
            _subscribers[index] = None


def _notify_subscribers(evt: dict) -> None:
    with _lock:
        for cb in _subscribers:
            if cb:
                try:
                    cb(evt)
                except Exception:
                    pass


class SSEBroadcaster:
    """Manages SSE (Server-Sent Events) clients for real-time push to Web UI."""

    def __init__(self) -> None:
        self._clients: list[queue.Queue] = []
        self._lock: threading.Lock = threading.Lock()

    def add_client(self, q: queue.Queue) -> None:
        with self._lock:
            self._clients.append(q)

    def remove_client(self, q: queue.Queue) -> None:
        with self._lock:
            if q in self._clients:
                self._clients.remove(q)

    def broadcast(self, evt: dict) -> None:
        data: str = json.dumps(evt)
        dead: list[queue.Queue] = []
        with self._lock:
            for q in self._clients:
                try:
                    q.put_nowait(data)
                except queue.Full:
                    dead.append(q)
            for q in dead:
                self._clients.remove(q)

    @property
    def client_count(self) -> int:
        with self._lock:
            return len(self._clients)


_sse = SSEBroadcaster()


def get_sse_broadcaster():
    return _sse


# Wire SSE broadcaster into event subscription
def _sse_forward(evt):
    _sse.broadcast(evt)


subscribe(_sse_forward)
