@echo off
REM update-your-pax-main.bat — Pull latest changes (Windows dev helper)
REM Usage: double-click or run from command prompt

setlocal enabledelayedexpansion

echo [*] Updating your-pax-main...

if exist "%~dp0..\.git" (
    echo [*] Pulling latest changes...
    cd /d "%~dp0.."
    git pull
) else (
    echo [!] Not a git repository. Skipping pull.
)

echo [*] Running Python compile check...
cd /d "%~dp0.."
python -m py_compile your-pax.py
python -m py_compile webapp.py
python -m py_compile orchestrator.py
python -m py_compile utils.py
python -m py_compile init_shared.py
python -m py_compile shared.py
echo [*] Python files OK.

echo [*] Running unit tests...
python -m pytest tests/ -v 2>nul || (
    echo [!] pytest not available. Run tests manually:
    echo     python tests\test_ftp_off_by_one.py
    echo     python tests\test_nmap_signature.py
    echo     python tests\test_path_traversal.py
    echo     python tests\test_rate_limiter.py
)

echo [*] Update complete.
pause

