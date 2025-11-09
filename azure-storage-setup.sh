#!/bin/bash
# =============================================================================
# Script de Configuración de Azure Blob Storage para Backend
# =============================================================================
# Este script configura las variables de entorno necesarias para que el backend
# use Azure Blob Storage para guardar las imágenes de productos.
# =============================================================================

set -e  # Salir si hay algún error

echo "🔧 CONFIGURACIÓN DE AZURE BLOB STORAGE PARA BACKEND"
echo "=================================================="
echo ""

# =============================================================================
# PASO 1: Verificar que tengas los datos necesarios
# =============================================================================

echo "📋 Necesitarás los siguientes datos de tu Storage Account:"
echo "   1. Nombre del Storage Account (ej: miprojectstorage)"
echo "   2. Connection String (lo obtienes de Azure Portal > Storage Account > Access Keys)"
echo "   3. Nombre del contenedor (ej: imagenes)"
echo ""

# =============================================================================
# PASO 2: Solicitar datos al usuario
# =============================================================================

read -p "📦 Nombre del Storage Account: " STORAGE_ACCOUNT_NAME
read -p "🔑 Connection String: " CONNECTION_STRING
read -p "📁 Nombre del contenedor (default: imagenes): " CONTAINER_NAME

# Usar valor por defecto si no se proporciona
CONTAINER_NAME=${CONTAINER_NAME:-imagenes}

echo ""
echo "✅ Configuración recibida:"
echo "   - Storage Account: $STORAGE_ACCOUNT_NAME"
echo "   - Contenedor: $CONTAINER_NAME"
echo "   - Connection String: ***********"
echo ""

# =============================================================================
# PASO 3: Actualizar Azure Container App con las variables
# =============================================================================

read -p "🚀 ¿Desplegar en Azure Container App? (y/n): " DEPLOY_CHOICE

if [[ "$DEPLOY_CHOICE" == "y" || "$DEPLOY_CHOICE" == "Y" ]]; then
    echo ""
    echo "🔄 Actualizando Container App con configuración de Azure Storage..."
    
    # Obtener el nombre del backend (ajusta si es diferente)
    BACKEND_APP_NAME="mplink-backend"
    RESOURCE_GROUP="rg-app-container"
    
    read -p "📝 Nombre del Container App (default: $BACKEND_APP_NAME): " INPUT_APP_NAME
    BACKEND_APP_NAME=${INPUT_APP_NAME:-$BACKEND_APP_NAME}
    
    read -p "📝 Resource Group (default: $RESOURCE_GROUP): " INPUT_RG
    RESOURCE_GROUP=${INPUT_RG:-$RESOURCE_GROUP}
    
    echo ""
    echo "🔧 Configurando variables de entorno..."
    
    az containerapp update \
      --name "$BACKEND_APP_NAME" \
      --resource-group "$RESOURCE_GROUP" \
      --set-env-vars \
        "AZURE_STORAGE_ENABLED=true" \
        "AZURE_STORAGE_CONNECTION_STRING=$CONNECTION_STRING" \
        "AZURE_STORAGE_CONTAINER_NAME=$CONTAINER_NAME"
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "✅ ¡Configuración completada exitosamente!"
        echo ""
        echo "📋 Variables configuradas:"
        echo "   - AZURE_STORAGE_ENABLED=true"
        echo "   - AZURE_STORAGE_CONNECTION_STRING=***"
        echo "   - AZURE_STORAGE_CONTAINER_NAME=$CONTAINER_NAME"
        echo ""
        echo "🔄 El Container App se está reiniciando con la nueva configuración..."
        echo "⏱️  Espera 1-2 minutos para que los cambios tomen efecto."
        echo ""
        echo "🧪 Para verificar:"
        echo "   1. Abre tu frontend"
        echo "   2. Crea/edita un producto con imágenes"
        echo "   3. Las imágenes deberían guardarse en Azure Blob Storage"
        echo "   4. Verifica en Azure Portal > Storage Account > Contenedores > $CONTAINER_NAME"
    else
        echo ""
        echo "❌ Error al actualizar el Container App"
        echo "   Verifica que el nombre y resource group sean correctos"
        exit 1
    fi
else
    echo ""
    echo "📝 Configuración manual:"
    echo "   Ejecuta este comando en Azure CLI:"
    echo ""
    echo "   az containerapp update \\"
    echo "     --name mplink-backend \\"
    echo "     --resource-group rg-app-container \\"
    echo "     --set-env-vars \\"
    echo "       \"AZURE_STORAGE_ENABLED=true\" \\"
    echo "       \"AZURE_STORAGE_CONNECTION_STRING=$CONNECTION_STRING\" \\"
    echo "       \"AZURE_STORAGE_CONTAINER_NAME=$CONTAINER_NAME\""
    echo ""
fi

echo ""
echo "📖 INFORMACIÓN ADICIONAL:"
echo "========================"
echo ""
echo "🔹 Nivel de acceso del contenedor:"
echo "   - Si quieres que las imágenes sean públicas (accesibles via URL):"
echo "     Azure Portal > Storage Account > Contenedores > $CONTAINER_NAME"
echo "     → Cambiar nivel de acceso a 'Blob (anonymous read access for blobs only)'"
echo ""
echo "🔹 El backend ahora:"
echo "   - Guarda nuevas imágenes en Azure Blob Storage"
echo "   - Retorna URLs públicas de Azure (si el contenedor es público)"
echo "   - Las imágenes persisten aunque el contenedor se reinicie"
echo ""
echo "🔹 Costos de Azure Storage:"
echo "   - Almacenamiento: ~\$0.02 USD por GB/mes"
echo "   - Transacciones: ~\$0.0004 USD por 10,000 operaciones"
echo "   - Muy económico para aplicaciones pequeñas/medianas"
echo ""
echo "✅ ¡Configuración completada!"
