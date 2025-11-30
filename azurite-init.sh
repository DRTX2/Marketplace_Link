#!/bin/bash
# =============================================================================
# Script de Inicialización de Azurite (Emulador local de Azure Blob Storage)
# =============================================================================
# Este script crea el contenedor de blob storage en Azurite al iniciar
# =============================================================================

set -e

echo "🚀 Inicializando contenedor en Azurite..."

# Esperar a que Azurite esté disponible
echo "⏳ Esperando a que Azurite esté listo..."
max_attempts=30
attempt=0

while [ $attempt -lt $max_attempts ]; do
    if curl -s http://localhost:10000/devstoreaccount1?comp=list > /dev/null 2>&1; then
        echo "✅ Azurite está listo"
        break
    fi
    attempt=$((attempt + 1))
    echo "   Intento $attempt de $max_attempts..."
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo "❌ Timeout esperando a Azurite"
    exit 1
fi

# Crear contenedor usando Azure CLI (necesita estar instalado)
# O usar curl directamente con la API REST de Azurite

CONTAINER_NAME=${AZURE_STORAGE_CONTAINER_NAME:-marketplace-images}

echo "📦 Creando contenedor: $CONTAINER_NAME"

# Usar curl para crear el contenedor via REST API
# Fecha actual en formato RFC1123 (requerido por Azure Storage API)
DATE=$(date -u +"%a, %d %b %Y %H:%M:%S GMT")

curl -X PUT "http://localhost:10000/devstoreaccount1/$CONTAINER_NAME?restype=container" \
    -H "x-ms-date: $DATE" \
    -H "x-ms-version: 2021-08-06" \
    -H "x-ms-blob-public-access: blob" \
    -v

if [ $? -eq 0 ]; then
    echo "✅ Contenedor '$CONTAINER_NAME' creado exitosamente"
else
    echo "⚠️  El contenedor podría ya existir o hubo un error (ignorando)"
fi

echo "🎉 Azurite está listo para usar!"
echo ""
echo "📝 Información de conexión:"
echo "   - Blob endpoint: http://localhost:10000/devstoreaccount1"
echo "   - Contenedor: $CONTAINER_NAME"
echo "   - Account: devstoreaccount1"
echo ""
