package com.VetSphere.Backend.modules.clinic.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(
            "pdf", "png", "jpg", "jpeg", "webp", "doc", "docx", "xls", "xlsx"
    );
    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) throws IOException {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath();
        Files.createDirectories(this.uploadDir);
    }

    public String guardar(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("Archivo vacio");
        }
        String original = file.getOriginalFilename() == null ? "archivo" : file.getOriginalFilename();
        String nombreLimpio = Paths.get(original).getFileName().toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        validarExtension(nombreLimpio);
        String nombre = UUID.randomUUID() + "-" + nombreLimpio;
        Path destino = uploadDir.resolve(nombre).normalize();
        if (!destino.startsWith(uploadDir)) {
            throw new IOException("Ruta de archivo invalida");
        }
        file.transferTo(destino);
        return "/uploads/" + nombre;
    }

    public void eliminar(String url) throws IOException {
        if (url == null || url.isBlank()) return;
        String nombre = Paths.get(url).getFileName().toString();
        Path destino = uploadDir.resolve(nombre).normalize();
        if (!destino.startsWith(uploadDir)) {
            throw new IOException("Ruta de archivo invalida");
        }
        Files.deleteIfExists(destino);
    }

    private void validarExtension(String nombre) throws IOException {
        int punto = nombre.lastIndexOf('.');
        if (punto < 0 || punto == nombre.length() - 1) {
            throw new IOException("Archivo sin extension valida");
        }
        String extension = nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IOException("Tipo de archivo no permitido");
        }
    }
}


