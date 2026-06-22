function escapeHtml(str) {
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(str));
    return div.innerHTML;
}

const logConsole = document.getElementById('log-console');
const mainToolbar = document.querySelector('.toolbar');
const toggleButton = document.getElementById('toggle-toolbar');
let fontSize = 16; // size for desktop
const maxLines = 2000; // Number of lines to keep in the console
const fileColors = new Map();
const levelClasses = {
    "DEBUG": "debug",
    "INFO": "info",
    "WARNING": "warning",
    "ERROR": "error",
    "CRITICAL": "critical",
    "SUCCESS": "success"
};

// Adjust font size based on device type
if (/Mobi|Android/i.test(navigator.userAgent)) {
    fontSize = 7; // size for mobile
}
logConsole.style.fontSize = fontSize + 'px';

function getRandomColor() {
    const letters = '89ABCDEF';  // Using only hex value for lighter colors
    let color = '#';
    for (let i = 0; i < 6; i++) {
        color += letters[Math.floor(Math.random() * letters.length)];
    }
    return color;
}

let logInterval;
let isConsoleOn = false;

function fetchLogs() {
    fetch('/get_logs')
        .then(response => response.text())
        .then(data => {
            const lines = data.split('\n');
            const newContent = [];

            lines.forEach(line => {
                let modifiedLine = escapeHtml(line);
                const regexFile = /(\w+\.py)/g;
                let matchFile;
                while ((matchFile = regexFile.exec(line)) !== null) {
                    const fileName = matchFile[1];
                    if (line.includes('==>') || line.includes('<==')) 
                    return;
                    if (!fileColors.has(fileName)) {
                        fileColors.set(fileName, getRandomColor());
                    }
                    const escapedFileName = escapeHtml(fileName);
                    modifiedLine = modifiedLine.replace(escapedFileName, `<span style="color: ${fileColors.get(fileName)};">${escapedFileName}</span>`);
                }

                const regexLevel = /\b(DEBUG|INFO|WARNING|ERROR|CRITICAL|SUCCESS)\b/g;
                modifiedLine = modifiedLine.replace(regexLevel, (match) => {
                    return `<span class="${levelClasses[match]}">${match}</span>`;
                });

                const regexLineNumber = /^\d+/;
                modifiedLine = modifiedLine.replace(regexLineNumber, (match) => {
                    return `<span class="line-number">${match}</span>`;
                });

                const regexNumbers = /\b\d+\b/g;
                modifiedLine = modifiedLine.replace(regexNumbers, (match) => {
                    return `<span class="number">${match}</span>`;
                });

                newContent.push(modifiedLine);
            });

            logConsole.innerHTML += newContent.join('<br>') + '<br>';

            let allLines = logConsole.innerHTML.split('<br>');
            if (allLines.length > maxLines) {
                allLines = allLines.slice(allLines.length - maxLines);
                logConsole.innerHTML = allLines.join('<br>');
            }
            logConsole.scrollTop = logConsole.scrollHeight;
        })
        .catch(error => console.error('Error fetching logs:', error));
}

// setInterval(fetchLogs, 1500); /
function startConsole() {
    // Start fetching logs every 1.5 seconds
    logInterval = setInterval(fetchLogs, 3000); // Fetch logs every 3 seconds
}
function stopConsole() {
    clearInterval(logInterval);
}
function toggleConsole() {
    const toggleImage = document.getElementById('toggle-console-image');
    
    if (isConsoleOn) {
        stopConsole();
        toggleImage.src = '/web/images/off.png';
    } else {
        startConsole();
        toggleImage.src = '/web/images/on.png';
    }
    
    isConsoleOn = !isConsoleOn;
}
function adjustFontSize(change) {
    fontSize += change;
    logConsole.style.fontSize = fontSize + 'px';
}

document.addEventListener('DOMContentLoaded', () => {
    const mainToolbar = document.getElementById('mainToolbar');
    const toggleButton = document.getElementById('toggle-toolbar');
    const toggleIcon = document.getElementById('toggle-icon');

    toggleButton.addEventListener('click', toggleToolbar);
});

function toggleToolbar() {
    const mainToolbar = document.getElementById('mainToolbar');
    const toggleButton = document.getElementById('toggle-toolbar');
    const toggleIcon = document.getElementById('toggle-icon');
    const isOpen = toggleButton.getAttribute('data-open') === 'true';
    if (isOpen) {
        mainToolbar.classList.add('hidden');
        toggleIcon.src = '/web/images/reveal.png';
        toggleButton.setAttribute('data-open', 'false');
    } else {
        mainToolbar.classList.remove('hidden');
        toggleIcon.src = '/web/images/hide.png';
        toggleButton.setAttribute('data-open', 'true');
    }
    toggleConsoleSize();
}

function toggleConsoleSize() {
}

function loadDropdown() {
    const dropdownContent = `
        <div class="dropdown">
            <button type="button" class="toolbar-button" onclick="toggleDropdown()" data-open="false">
                <img src="/web/images/manual_icon.png" alt="Icon_actions" style="height: 50px;">
            </button>
            <div class="dropdown-content">
                <button type="button" onclick="clear_files()">Clear Files</button>
                <button type="button" onclick="clear_files_light()">Clear Files Light</button>
                <button type="button" onclick="reboot_system()">Reboot</button>
                <button type="button" onclick="disconnect_wifi()">Disconnect Wi-Fi</button>
                <button type="button" onclick="shutdown_system()">Shutdown</button>
                <button type="button" onclick="restart_your_pax_service()">Restart your-pax Service</button>
                <button type="button" onclick="backup_data()">Backup</button>
                <button type="button" onclick="restore_data()">Restore</button>
                <button type="button" onclick="stop_orchestrator()">Stop Orchestrator</button>
                <button type="button" onclick="start_orchestrator()">Start Orchestrator</button>
                <button type="button" onclick="initialize_csv()">Create Livestatus, Actions & Netkb CSVs</button>
                <hr style="border-color:#333;margin:4px 0;">
                <button type="button" onclick="switchMode('web_only')" style="color:#00ff88;">Mode: Web Only</button>
                <button type="button" onclick="switchMode('app_only')" style="color:#ffaa00;">Mode: App Only</button>
                <button type="button" onclick="switchMode('web_app')" style="color:#aa66ff;">Mode: Web + App</button>
            </div>
        </div>
    `;
    document.getElementById('dropdown-container').innerHTML = dropdownContent;
}

function switchMode(mode) {
    if (!confirm('Switch to ' + mode + ' mode? This will restart services.')) return;
    fetch('/switch_mode', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({mode: mode})
    })
    .then(r => r.json())
    .then(data => {
        if (data.status === 'success') {
            alert('Mode switched to ' + mode);
            loadCurrentMode();
        } else {
            alert('Switch failed: ' + data.message);
        }
    })
    .catch(e => alert('Error: ' + e.message));
}

function loadCurrentMode() {
    fetch('/load_config')
        .then(r => r.json())
        .then(cfg => {
            const mode = cfg.connection_mode || 'web_app';
            const el = document.getElementById('modeDisplay');
            if (el) {
                const labels = {web_only: 'Web Only', app_only: 'App Only', web_app: 'Web + App'};
                el.textContent = 'Mode: ' + (labels[mode] || mode);
                el.style.color = mode === 'web_only' ? '#00ff88' : mode === 'app_only' ? '#ffaa00' : '#aa66ff';
            }
        })
        .catch(() => {});
}

function loadYourpaxDropdown() {
    const yourpaxDropdownContent = `
        <div class="dropdown yourpax-dropdown">
            <button type="button" class="toolbar-button" onclick="toggleYourpaxDropdown()" data-open="false">
                <img src="/web/images/your-pax_icon.png" alt="Icon_yourpax" style="height: 50px;">
            </button>
            <div class="dropdown-content">
                <img id="screenImage_Home"  onclick="window.location.href='/your-pax.html'" src="screen.png" alt="your-pax" style="width: 100%;">
            </div>
        </div>
    `;
    document.getElementById('yourpax-dropdown-container').innerHTML = yourpaxDropdownContent;
    startLiveview(); // Start live view when your-pax dropdown is loaded
}

function connectSSE() {
    if (window._sseConnected) return;
    window._sseConnected = true;
    const evtSource = new EventSource('/events');
    evtSource.onmessage = function(e) {
        try {
            const evt = JSON.parse(e.data);
            if (evt.event === 'system_status' && evt.data) {
                const cpu = evt.data.cpu_usage;
                const ram = evt.data.ram_free_mb + '/' + evt.data.ram_total_mb + 'MB';
                document.getElementById('btDisplayStatus').textContent = 'CPU: ' + cpu + '% | RAM: ' + ram;
            }
            if (evt.event === 'wifi_handshake_captured' && evt.data) {
                const popup = document.getElementById('popupContainer');
                if (popup) popup.innerHTML = '<div style="background:#1a3a1a;border:1px solid #00ff88;padding:8px;margin:4px;border-radius:4px;color:#00ff88;">Handshake: ' + (evt.data.bssid || '') + '</div>';
                setTimeout(() => { if (popup) popup.innerHTML = ''; }, 5000);
            }
        } catch(e) {}
    };
    evtSource.onerror = function() {
        window._sseConnected = false;
        setTimeout(connectSSE, 5000);
    };
}

// Call the function to load the dropdowns when the DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    loadDropdown();
    loadYourpaxDropdown();
    loadCurrentMode();
    connectSSE();
});


function clear_files() {
    fetch('/clear_files', { method: 'POST' })
        .then(response => response.json())
        .then(data => alert(data.message))
        .catch(error => alert('Failed to clear files: ' + error.message));
}

function clear_files_light() {
    fetch('/clear_files_light', { method: 'POST' })
        .then(response => response.json())
        .then(data => alert(data.message))
        .catch(error => alert('Failed to clear files: ' + error.message));
}

function reboot_system() {
    fetch('/reboot', { method: 'POST' })
        .then(response => response.json())
        .then(data => alert(data.message))
        .catch(error => alert('Failed to reboot: ' + error.message));
}

function shutdown_system() {
    fetch('/shutdown', { method: 'POST' })
        .then(response => response.json())
        .then(data => alert(data.message))
        .catch(error => alert('Failed to shutdown: ' + error.message));
}

function restart_your_pax_service() {
    fetch('/restart_your_pax_service', { method: 'POST' })
        .then(response => response.json())
        .then(data => alert(data.message))
        .catch(error => alert('Failed to restart service: ' + error.message));
}

function backup_data() {
    fetch('/backup', { method: 'POST' })
        .then(response => response.json())
        .then(data => {
            if (data.status === 'success') {
                const link = document.createElement('a');
                link.href = data.url;
                link.download = data.filename;
                link.click();
                alert('Backup completed successfully');
            } else {
                alert('Backup failed: ' + data.message);
            }
        })
        .catch(error => alert('Backup failed: ' + error.message));
}

function restore_data() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.zip';
    input.onchange = () => {
        const file = input.files[0];
        const formData = new FormData();
        formData.append('file', file);

        fetch('/restore', {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(data => alert(data.message))
        .catch(error => alert('Restore failed: ' + error.message));
    };
    input.click();
}

function stop_orchestrator() {
    fetch('/stop_orchestrator', { method: 'POST' })
        .then(response => response.json())
        .then(data => alert(data.message))
        .catch(error => alert('Failed to stop orchestrator: ' + error.message));
}

function start_orchestrator() {
    fetch('/start_orchestrator', { method: 'POST' })
        .then(response => response.json())
        .then(data => alert(data.message))
        .catch(error => alert('Failed to start orchestrator: ' + error.message));
}

function disconnect_wifi() {
    fetch('/disconnect_wifi', { method: 'POST' })
        .then(response => response.json())
        .then(data => alert(data.message))
        .catch(error => alert('Failed to disconnect: ' + error.message));
}

function initialize_csv() {
    fetch('/initialize_csv', { method: 'POST' })
        .then(response => response.json())
        .then(data => alert(data.message))
        .catch(error => alert('Failed to initialize CSV: ' + error.message));
}


// actions.js

let imageIntervalId;
let intervalId;
const delay = 2000 // Adjust this value to match your delay

let lastUpdate = 0;

function updateImage() {
    const now = Date.now();
    if (now - lastUpdate >= delay) {
        lastUpdate = now;
        const image = document.getElementById("screenImage_Home");
        const newImage = new Image();
        newImage.onload = function() {
            image.src = newImage.src; // Update only if the new image loads successfully
        };
        newImage.onerror = function() {
            console.warn("New image could not be loaded, keeping the previous image.");
        };
        newImage.src = "screen.png?t=" + new Date().getTime(); // Prevent caching
    }
}

function startLiveview() {
    updateImage(); // Immediately update the image
    intervalId = setInterval(updateImage, delay); // Then update at the specified interval
}

function stopLiveview() {
    clearInterval(intervalId);
}

// Dropdown toggle logic for your-pax
function toggleYourpaxDropdown() {
    const dropdown = document.querySelector('.yourpax-dropdown');
    const button = document.querySelector('.yourpax-button');
    const isOpen = button.getAttribute('data-open') === 'true';

    if (isOpen) {
        dropdown.classList.remove('show');
        button.setAttribute('data-open', 'false');
        stopLiveview(); // Stop image refresh when closing
    } else {
        dropdown.classList.add('show');
        button.setAttribute('data-open', 'true');
        startLiveview(); // Start image refresh when opening
    }
}

function closeYourpaxDropdownIfOpen(event) {
    const dropdown = document.querySelector('.yourpax-dropdown');
    const button = document.querySelector('.yourpax-button');
    const isOpen = button.getAttribute('data-open') === 'true';

    if (!event.target.closest('.yourpax-dropdown') && isOpen) {
        dropdown.classList.remove('show');
        button.setAttribute('data-open', 'false');
        stopLiveview(); // Stop image refresh when closing
    }
}

document.addEventListener('click', closeYourpaxDropdownIfOpen);
document.addEventListener('touchstart', closeYourpaxDropdownIfOpen);

// Existing logic for Actions dropdown
function toggleDropdown() {
    const dropdown = document.querySelector('.dropdown');
    const button = document.querySelector('.action-button');
    const isOpen = button.getAttribute('data-open') === 'true';

    if (isOpen) {
        dropdown.classList.remove('show');
        button.setAttribute('data-open', 'false');
    } else {
        dropdown.classList.add('show');
        button.setAttribute('data-open', 'true');
    }
}

function closeDropdownIfOpen(event) {
    const dropdown = document.querySelector('.dropdown');
    const button = document.querySelector('.action-button');
    const isOpen = button.getAttribute('data-open') === 'true';

    if (!event.target.closest('.dropdown') && isOpen) {
        dropdown.classList.remove('show');
        button.setAttribute('data-open', 'false');
    }
}

