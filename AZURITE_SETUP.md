# 🧪 Usando Azure Blob Storage en Local con Azurite

## ¿Qué es Azurite?

**Azurite** es el emulador oficial de Microsoft para Azure Storage. Te permite probar tu código que usa Azure Blob Storage sin necesidad de conectarte a Azure real, perfecto para desarrollo local.

---

## 🚀 Inicio Rápido

### 1. Levantar los servicios con Docker Compose

```bash
# Desde el directorio del proyecto backend
docker-compose up -d
```

Esto levantará:
- ✅ PostgreSQL (base de datos principal)
- ✅ PostgreSQL Test (base de datos de pruebas)
- ✅ **Azurite** (emulador de Azure Blob Storage)

### 2. Crear el contenedor de blobs

Ejecuta el script de inicialización para crear el contenedor `marketplace-images`:

```bash
bash azurite-init.sh
```

### 3. Verificar que funciona

Inicia tu aplicación Spring Boot:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Deberías ver en los logs:

```
✅ Azure Blob Storage inicializado correctamente. Contenedor: marketplace-images
```

### 4. Probar subida de imágenes

Usa tu API para subir una imagen de producto. La imagen se guardará en Azurite (localmente) pero tu código funcionará exactamente igual que si estuvieras usando Azure real.

---

## 🔍 Conexión y Configuración

### Connection String de Azurite (ya configurada)

```
DefaultEndpointsProtocol=http;
AccountName=devstoreaccount1;
AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;
BlobEndpoint=http://localhost:10000/devstoreaccount1;
```

> ⚠️ **Nota**: Esta connection string es la misma para todos los desarrolladores usando Azurite. Es solo para desarrollo local.

### Variables de entorno (`application-dev.yml`)

```yaml
azure:
  storage:
    enabled: true  # Cambiar a false para usar almacenamiento local tradicional
    connection-string: <connection-string-de-azurite>
    container-name: marketplace-images
```

---

## 🛠️ Herramientas para Explorar Azurite

### 1. Azure Storage Explorer (Recomendado)

Descarga: https://azure.microsoft.com/en-us/products/storage/storage-explorer/

1. Abre **Azure Storage Explorer**
2. Click en **Connect** → **Local storage emulator**
3. Usa estos valores:
   - **Display name**: Azurite Local
   - **Ports**: Blob=10000, Queue=10001, Table=10002
4. Podrás ver y gestionar tus blobs visualmente

### 2. Usando cURL (Comandos CLI)

```bash
# Listar contenedores
curl "http://localhost:10000/devstoreaccount1?comp=list"

# Listar blobs en un contenedor
curl "http://localhost:10000/devstoreaccount1/marketplace-images?restype=container&comp=list"
```

### 3. VS Code Extension: Azurite

Busca e instala la extensión "Azurite" en VS Code para control directo desde el editor.

---

## 🔄 Cambiar entre Local y Azure Real

### Modo 1: Azure Blob Storage (Azurite Local) ✅ Por defecto en dev

```yaml
azure:
  storage:
    enabled: true
    connection-string: <azurite-connection-string>
```

Tu código usa `AzureBlobStorageService` pero contra Azurite local.

### Modo 2: Almacenamiento Local (FileSystem)

```yaml
azure:
  storage:
    enabled: false
```

Tu código guarda archivos en `uploads/` (carpeta local).

### Modo 3: Azure Real (Producción)

```yaml
azure:
  storage:
    enabled: true
    connection-string: ${AZURE_STORAGE_CONNECTION_STRING}  # Connection string real de Azure
    container-name: marketplace-images-prod
```

---

## 📦 Endpoints de Azurite

| Servicio | Puerto | URL |
|----------|--------|-----|
| **Blob**  | 10000  | `http://localhost:10000/devstoreaccount1` |
| **Queue** | 10001  | `http://localhost:10001/devstoreaccount1` |
| **Table** | 10002  | `http://localhost:10002/devstoreaccount1` |

---

## 🧹 Limpieza de Datos

### Borrar todos los blobs en Azurite

```bash
# Opción 1: Reiniciar el contenedor
docker-compose restart azurite

# Opción 2: Borrar el volumen (elimina TODO)
docker-compose down -v
docker-compose up -d
bash azurite-init.sh
```

### Ver logs de Azurite

```bash
docker-compose logs -f azurite
```

---

## 🐛 Troubleshooting

### ❌ "Error al inicializar Azure Blob Storage"

**Solución**: Verifica que Azurite esté corriendo:

```bash
docker ps | grep azurite
```

Si no está corriendo:

```bash
docker-compose up -d azurite
```

### ❌ "Contenedor no existe"

**Solución**: Ejecuta el script de inicialización:

```bash
bash azurite-init.sh
```

### ❌ "Connection refused" en puerto 10000

**Solución**: Asegúrate de que el puerto 10000 no esté ocupado:

```bash
# Windows
netstat -ano | findstr :10000

# Linux/Mac
lsof -i :10000
```

---

## 🌐 URLs de las Imágenes

### En Local (Azurite)

```
http://localhost:10000/devstoreaccount1/marketplace-images/products/uuid-de-la-imagen.jpg
```

### En Producción (Azure Real)

```
https://tu-storage-account.blob.core.windows.net/marketplace-images/products/uuid-de-la-imagen.jpg
```

> 💡 **Importante**: Tu código ya maneja ambos casos automáticamente gracias a `FileStorageService`.

---

## ✅ Ventajas de usar Azurite

1. ✅ **Código idéntico** a producción
2. ✅ **Sin costos** de Azure durante desarrollo
3. ✅ **Sin internet** necesario
4. ✅ **Velocidad** de pruebas mucho más rápida
5. ✅ **Aislamiento** de datos de desarrollo vs producción
6. ✅ **Reset rápido** de datos de prueba

---

## 📚 Recursos Adicionales

- [Documentación oficial de Azurite](https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite)
- [Azure Blob Storage SDK for Java](https://learn.microsoft.com/en-us/java/api/overview/azure/storage-blob-readme)
- [Azure Storage Explorer](https://azure.microsoft.com/en-us/products/storage/storage-explorer/)

---

## 🔐 Diferencias con Azure Real

| Característica | Azurite Local | Azure Real |
|----------------|---------------|------------|
| **Connection String** | Fijo (devstoreaccount1) | Único por cuenta |
| **HTTPS** | No (HTTP) | Sí (HTTPS) |
| **Autenticación** | Clave fija | Clave rotable o Managed Identity |
| **CDN** | No disponible | Disponible |
| **Geo-replicación** | No | Sí |
| **Costo** | Gratis | De pago |

---

**¿Listo para producción?** Solo actualiza el `connection-string` en las variables de entorno de producción y ¡listo! 🚀
