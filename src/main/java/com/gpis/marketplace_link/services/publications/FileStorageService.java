package com.gpis.marketplace_link.services.publications;

import com.gpis.marketplace_link.exceptions.business.publications.UploadFolderException;
import com.gpis.marketplace_link.services.AzureBlobStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    @Value("${azure.storage.enabled:false}")
    private boolean azureStorageEnabled;

    @Autowired(required = false)
    private AzureBlobStorageService azureBlobStorageService;

    public FileStorageService() {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("📁 Directorio de uploads local creado: {}", this.fileStorageLocation);
        } catch (IOException ex) {
            throw new UploadFolderException("Error al crear el directorio de subida de archivos", ex);
        }
    }

    /**
     * Almacena un archivo usando Azure Blob Storage (si está habilitado) o sistema
     * de archivos local
     * Mejoras del fix/upload-images para manejo local más robusto
     * 
     * @param file archivo a almacenar
     * @return URL completa si usa Azure, o nombre del archivo si usa almacenamiento
     *         local
     */
    public String storeFile(MultipartFile file) {
        // Si Azure Storage está habilitado, usar Azure Blob Storage
        if (azureStorageEnabled && azureBlobStorageService != null) {
            try {
                String blobUrl = azureBlobStorageService.uploadFile(file, "products");
                log.info("✅ Archivo subido a Azure Blob Storage: {}", blobUrl);
                return blobUrl; // Retornar URL completa
            } catch (IOException ex) {
                log.error("❌ Error al subir archivo a Azure, usando almacenamiento local como fallback", ex);
                // Fallback a almacenamiento local si falla Azure
                return storeFileLocally(file);
            }
        }

        // Usar almacenamiento local
        return storeFileLocally(file);
    }

    /**
     * Almacena archivo en el sistema de archivos local
     * Lógica mejorada de fix/upload-images
     */
    private String storeFileLocally(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        String extension = "";

        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID().toString().concat(extension);

        try {
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation);
            log.info("📁 Archivo guardado localmente: {}", fileName);
            return fileName; // Solo retornar nombre del archivo
        } catch (IOException ex) {
            throw new UploadFolderException("No se pudo almacenar el archivo " + fileName, ex);
        }
    }

    /**
     * Obtiene la ruta del archivo en el sistema local
     */
    public Path getFilePath(String fileName) {
        return this.fileStorageLocation.resolve(fileName);
    }

    /**
     * Verifica si un archivo existe
     * Soporta tanto URLs de Azure como nombres de archivo locales
     */
    public boolean fileExists(String fileReference) {
        if (fileReference == null || fileReference.isEmpty()) {
            return false;
        }

        // Si es una URL de Azure Blob Storage (https o http para desarrollo local)
        if (azureStorageEnabled && (fileReference.startsWith("https://") || fileReference.startsWith("http://"))
                && azureBlobStorageService != null) {
            return azureBlobStorageService.fileExists(fileReference);
        }

        // Si es un archivo local
        Path filePath = getFilePath(fileReference);
        return Files.exists(filePath);
    }

    /**
     * Elimina un archivo
     * Soporta tanto URLs de Azure como nombres de archivo locales
     */
    public void deleteFile(String fileReference) {
        if (fileReference == null || fileReference.isEmpty()) {
            return;
        }

        try {
            // Si es una URL de Azure Blob Storage (https o http para desarrollo local)
            if (azureStorageEnabled && (fileReference.startsWith("https://") || fileReference.startsWith("http://"))
                    && azureBlobStorageService != null) {
                boolean deleted = azureBlobStorageService.deleteFile(fileReference);
                if (deleted) {
                    log.info("🗑️ Archivo eliminado de Azure Blob Storage: {}", fileReference);
                } else {
                    log.warn("⚠️ No se pudo eliminar el archivo de Azure: {}", fileReference);
                }
                return;
            }

            // Si es un archivo local
            Path filePath = getFilePath(fileReference);
            Files.deleteIfExists(filePath);
            log.info("🗑️ Archivo local eliminado: {}", fileReference);
        } catch (IOException ex) {
            throw new UploadFolderException("No se pudo eliminar el archivo " + fileReference, ex);
        }
    }

    /**
     * Obtiene la URL pública del archivo
     * Si usa Azure, retorna la URL directamente
     * Si usa almacenamiento local, retorna /uploads/{filename}
     */
    public String getFileUrl(String fileReference) {
        if (fileReference == null || fileReference.isEmpty()) {
            return null;
        }

        // Si ya es una URL completa (Azure), retornarla tal cual
        if (fileReference.startsWith("https://") || fileReference.startsWith("http://")) {
            return fileReference;
        }

        // Si es un archivo local, construir la URL relativa
        return "/uploads/" + fileReference;
    }

    /**
     * Verifica si está usando Azure Storage
     */
    public boolean isUsingAzureStorage() {
        return azureStorageEnabled && azureBlobStorageService != null;
    }
}
