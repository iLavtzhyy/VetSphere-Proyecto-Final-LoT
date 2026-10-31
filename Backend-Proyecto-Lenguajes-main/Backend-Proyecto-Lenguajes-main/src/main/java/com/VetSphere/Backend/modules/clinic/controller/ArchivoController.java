package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.shared.response.ApiResponse;
import com.VetSphere.Backend.modules.clinic.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/archivos")
@RequiredArgsConstructor
public class ArchivoController {
    private final FileStorageService fileStorageService;

    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<String> subir(@RequestPart MultipartFile archivo) throws IOException {
        return ApiResponse.ok("Archivo subido", fileStorageService.guardar(archivo));
    }
}

