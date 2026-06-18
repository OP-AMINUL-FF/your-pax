#init_shared.py
# Description:
# This file, init_shared.py, is responsible for initializing and providing access to shared data across different modules in the your-pax project.
#
# Key functionalities include:
# - Importing the `SharedData` class from the `shared` module.
# - Creating an instance of `SharedData` named `shared_data` that holds common configuration, paths, and other resources.
# - Ensuring that all modules importing `shared_data` will have access to the same instance, promoting consistency and ease of data management throughout the project.


class _LazySharedData:
    _instance = None

    def __getattr__(self, name):
        if self._instance is None:
            from shared import SharedData
            _LazySharedData._instance = SharedData()
        return getattr(self._instance, name)

    def __setattr__(self, name, value):
        if name == '_instance':
            super().__setattr__(name, value)
        else:
            if self._instance is None:
                from shared import SharedData
                _LazySharedData._instance = SharedData()
            setattr(self._instance, name, value)

shared_data = _LazySharedData()
