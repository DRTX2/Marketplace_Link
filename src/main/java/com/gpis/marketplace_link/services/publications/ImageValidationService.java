package com.gpis.marketplace_link.services.publications;

import com.gpis.marketplace_link.exceptions.business.publications.InvalidImageFileException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class ImageValidationService {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("png", "jpg", "jpeg", "gif", "webp");
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/png", "image/jpg", "image/jpeg", "image/gif", "image/webp");

    private static final List<String> IMAGE_SIGNATURES = Arrays.asList(
            "ffd8ff", // JPEG
            "89504e47", // PNG
            "47494638", // GIF
            "52494646" // WEBP
    );

    public void validateImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        for (MultipartFile file : files) {
            validateSingleImage(file);
        }
    }

    private void validateSingleImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidImageFileException("Se detectó un archivo vacío");
        }

        String fileName = file.getOriginalFilename();

        String extension = getFileExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new InvalidImageFileException(
                    String.format(
                            "El archivo '%s' tiene una extensión no permitida. Solo se permiten: PNG, JPG, JPEG, GIF, WEBP",
                            fileName));
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidImageFileException(
                    String.format("El archivo '%s' no es una imagen válida. Tipo detectado: %s", fileName,
                            contentType));
        }

        try {
            if (!hasValidImageSignature(file)) {
                throw new InvalidImageFileException(
                        String.format(
                                "El archivo '%s' no es una imagen real. Solo se permiten archivos de imagen válidos",
                                fileName));
            }
        } catch (IOException e) {
            throw new InvalidImageFileException(
                    String.format("Error al validar el archivo '%s': %s", fileName, e.getMessage()));
        }
    }

    private boolean hasValidImageSignature(MultipartFile file) throws IOException {
        byte[] fileBytes = file.getBytes();
        if (fileBytes.length < 4) {
            return false;
        }

        String fileSignature = bytesToHex(Arrays.copyOfRange(fileBytes, 0, Math.min(4, fileBytes.length)));

        for (String signature : IMAGE_SIGNATURES) {
            if (fileSignature.startsWith(signature.substring(0, Math.min(6, signature.length())))) {
                return true;
            }
        }

        return false;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }
}
