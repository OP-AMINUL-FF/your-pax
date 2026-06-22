@echo off
REM generate_hashes.bat — Generate SHA256 hash manifest (Windows)
REM Usage: scripts\generate_hashes.bat [output_file]

setlocal enabledelayedexpansion

if "%1"=="" (
    set OUTPUT=con
) else (
    set OUTPUT=%1
)

echo ============================================ > %OUTPUT%
echo  your-pax v1.1.1-alpha - SHA256 Hash Manifest >> %OUTPUT%
echo  Generated: %DATE% %TIME% >> %OUTPUT%
echo ============================================ >> %OUTPUT%
echo. >> %OUTPUT%

echo --- Core Python --- >> %OUTPUT%
for %%f in (your-pax.py webapp.py orchestrator.py shared.py init_shared.py utils.py logger.py event_bus.py) do (
    if exist %%f (
        for /f "usebackq delims=" %%h in (`certutil -hashfile %%f SHA256 ^| find /v "hash" ^| find /v "CertUtil"`) do set "hash=%%h"
        for %%s in (%%~zf) do set size=%%s
        echo  !hash!  %%f  (!size! bytes) >> %OUTPUT%
    )
)

echo. >> %OUTPUT%
echo --- Action Modules --- >> %OUTPUT%
for %%f in (actions\*.py) do (
    if exist %%f (
        for /f "usebackq delims=" %%h in (`certutil -hashfile %%f SHA256 ^| find /v "hash" ^| find /v "CertUtil"`) do set "hash=%%h"
        for %%s in (%%~zf) do set size=%%s
        echo  !hash!  %%f  (!size! bytes) >> %OUTPUT%
    )
)

echo. >> %OUTPUT%
echo --- Web UI --- >> %OUTPUT%
for %%f in (web\*.html web\scripts\*.js) do (
    if exist %%f (
        for /f "usebackq delims=" %%h in (`certutil -hashfile %%f SHA256 ^| find /v "hash" ^| find /v "CertUtil"`) do set "hash=%%h"
        for %%s in (%%~zf) do set size=%%s
        echo  !hash!  %%f  (!size! bytes) >> %OUTPUT%
    )
)

echo. >> %OUTPUT%
echo ============================================ >> %OUTPUT%
echo  End of Manifest >> %OUTPUT%
echo ============================================ >> %OUTPUT%
echo [*] Hash manifest written to %OUTPUT%

