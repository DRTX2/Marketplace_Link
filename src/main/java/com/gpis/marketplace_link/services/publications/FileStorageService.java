package com.gpis.marketplace_link.services.publications;

import com.gpis.marketplace_link.exceptions.business.publications.UploadFolderException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService() {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
            try {
                Files.createDirectories(this.fileStorageLocation);
        }catch (IOException ex){
            throw new UploadFolderException("Error al crear el directorio de subida de archivos",ex);
        }
    }

    public String storeFile(MultipartFile file){
        String originalFileName = file.getOriginalFilename();
        String extension="";

        if(originalFileName != null && originalFileName.contains(".")){
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));

        }

        String fileName = UUID.randomUUID().toString().concat(extension);

        try{
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(),targetLocation);
            return fileName;
        }catch (IOException ex){
            throw new UploadFolderException("No se pudo almacenar el archivo "+fileName,ex);
        }

    }
    public Path getFilePath(String fileName){
        return this.fileStorageLocation.resolve(fileName);
    }

    public   boolean fileExists(String fileName){
        Path filePath = getFilePath(fileName);
        return Files.exists(filePath);
    }


    public void deleteFile(String fileName){
        try{
            Path filePath = getFilePath(fileName);
            Files.deleteIfExists(filePath);
        }catch (IOException ex){
            throw new UploadFolderException("No se pudo eliminar el archivo "+fileName,ex);
        }
    }
}
