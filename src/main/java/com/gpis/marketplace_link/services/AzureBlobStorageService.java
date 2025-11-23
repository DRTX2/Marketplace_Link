package com.gpis.marketplace_link.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.PublicAccessType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
public class AzureBlobStorageService {
    @Value("${azure.storage.connection-string}")
    private String connectionString;
    @Value("${azure.storage.container-name}")
    private String containerName;
    @Value("${azure.storage.enabled:true}")
    private boolean azureStorageEnabled;
    private BlobServiceClient blobServiceClient;
    private BlobContainerClient containerClient;

    @PostConstruct
    public void init() {
        if (!azureStorageEnabled) {
            log.info("Azure Blob Storage está deshabilitado. Usando almacenamiento local.");
            return;
        }

        // Validar que el connection string no esté vacío
        if (connectionString == null || connectionString.trim().isEmpty()) {
            log.warn("⚠️ AZURE_STORAGE_ENABLED=true pero AZURE_STORAGE_CONNECTION_STRING está vacío. Usando almacenamiento local.");
            return;
        }

        try {
            // Crear cliente de Azure Blob Storage
            this.blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            // Obtener o crear el contenedor
            this.containerClient = blobServiceClient.getBlobContainerClient(containerName);
            
            if (!containerClient.exists()) {
                log.info("Creando contenedor: {}", containerName);
                containerClient.create();
                // Configurar acceso público para lectura de blobs (opcional)
                // Si quieres que las imágenes sean accesibles directamente via URL
                containerClient.setAccessPolicy(PublicAccessType.BLOB, null);
            }

            log.info("✅ Azure Blob Storage inicializado correctamente. Contenedor: {}", containerName);
        } catch (Exception e) {
            log.error("❌ Error al inicializar Azure Blob Storage: {}", e.getMessage());
            log.warn("Continuando con almacenamiento local debido al error en Azure Storage.");
            // NO lanzar excepción - permitir que la app continúe con almacenamiento local
        }
    }

    public String uploadFile(MultipartFile file, String directory) throws IOException {
        if (!azureStorageEnabled) {
            throw new UnsupportedOperationException("Azure Storage está deshabilitado");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        // Generar nombre único para el archivo
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf("."))
            : "";
        
        String blobName = directory + "/" + UUID.randomUUID() + extension;

        try {
            // Obtener el BlobClient
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            // Configurar headers HTTP para el blob
            BlobHttpHeaders headers = new BlobHttpHeaders()
                    .setContentType(file.getContentType());

            // Subir el archivo
            try (InputStream inputStream = file.getInputStream()) {
                blobClient.upload(inputStream, file.getSize(), true);
                blobClient.setHttpHeaders(headers);
            }

            // Retornar la URL pública del blob (decodificada para evitar %2F)
            String blobUrl = blobClient.getBlobUrl();
            // Decodificar la URL para que tenga "/" en lugar de "%2F"
            String decodedUrl = java.net.URLDecoder.decode(blobUrl, java.nio.charset.StandardCharsets.UTF_8);
            log.info("✅ Archivo subido exitosamente: {}", decodedUrl);
            return decodedUrl;

        } catch (Exception e) {
            log.error("❌ Error al subir archivo a Azure Blob Storage: {}", e.getMessage(), e);
            throw new IOException("Error al subir archivo a Azure Storage", e);
        }
    }
    public boolean deleteFile(String blobUrl) {
        if (!azureStorageEnabled) {
            return false;
        }

        try {
            // Extraer el nombre del blob de la URL
            String blobName = extractBlobNameFromUrl(blobUrl);
            
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            if (blobClient.exists()) {
                blobClient.delete();
                log.info("✅ Archivo eliminado exitosamente: {}", blobName);
                return true;
            } else {
                log.warn("⚠️ El archivo no existe: {}", blobName);
                return false;
            }

        } catch (Exception e) {
            log.error("❌ Error al eliminar archivo de Azure Blob Storage: {}", e.getMessage(), e);
            return false;
        }
    }
    public boolean fileExists(String blobUrl) {
        if (!azureStorageEnabled) {
            return false;
        }

        try {
            String blobName = extractBlobNameFromUrl(blobUrl);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            return blobClient.exists();
        } catch (Exception e) {
            log.error("Error al verificar existencia del archivo: {}", e.getMessage());
            return false;
        }
    }
    private String extractBlobNameFromUrl(String blobUrl) {
        if (blobUrl == null || blobUrl.isEmpty()) {
            throw new IllegalArgumentException("La URL del blob no puede ser nula o vacía");
        }

        try {
            // Formato: https://{account}.blob.core.windows.net/{container}/{blobName}
            String[] parts = blobUrl.split("/" + containerName + "/");
            if (parts.length > 1) {
                return parts[1];
            }
            throw new IllegalArgumentException("URL de blob inválida: " + blobUrl);
        } catch (Exception e) {
            log.error("Error al extraer nombre del blob de URL: {}", blobUrl, e);
            throw new IllegalArgumentException("URL de blob inválida", e);
        }
    }
    public String getContainerUrl() {
        return containerClient != null ? containerClient.getBlobContainerUrl() : null;
    }
    public boolean isEnabled() {
        return azureStorageEnabled;
    }
}
