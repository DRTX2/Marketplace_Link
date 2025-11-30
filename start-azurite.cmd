@echo off
REM =============================================================================
REM Script de Inicio Rápido de Azurite para Windows
REM =============================================================================

echo.
echo ========================================
echo  AZURITE - Azure Storage Emulator Local
echo ========================================
echo.

echo [1/3] Iniciando servicios con Docker Compose...
docker-compose up -d

echo.
echo [2/3] Esperando a que Azurite este listo...
timeout /t 5 /nobreak > nul

echo.
echo [3/3] Creando contenedor marketplace-images...
echo.

REM Ejecutar el script bash usando Git Bash (si esta disponible)
where bash >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    bash azurite-init.sh
) else (
    echo WARNING: Git Bash no encontrado. Creando contenedor manualmente...
    echo.
    
    REM Crear contenedor usando curl (si esta disponible)
    where curl >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        curl -X PUT "http://localhost:10000/devstoreaccount1/marketplace-images?restype=container" -H "x-ms-date: %date%" -H "x-ms-version: 2021-08-06" -H "x-ms-blob-public-access: blob"
        echo.
        echo Contenedor creado!
    ) else (
        echo ERROR: curl no encontrado. Por favor instala Git Bash o cURL.
        echo O crea el contenedor manualmente usando Azure Storage Explorer.
    )
)

echo.
echo ========================================
echo  AZURITE LISTO!
echo ========================================
echo.
echo  - Blob Storage: http://localhost:10000
echo  - Contenedor: marketplace-images
echo  - Account: devstoreaccount1
echo.
echo Puedes iniciar tu aplicacion Spring Boot con:
echo   mvnw spring-boot:run -Dspring-boot.run.profiles=dev
echo.
echo Para explorar los blobs usa Azure Storage Explorer:
echo   https://azure.microsoft.com/products/storage/storage-explorer/
echo.
pause
