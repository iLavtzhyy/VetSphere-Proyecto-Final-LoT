package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.Rol;
import com.VetSphere.Backend.modules.clinic.repository.RolRepository;
import com.VetSphere.Backend.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolController {
    private final RolRepository rolRepository;

    @GetMapping
    public ApiResponse<List<Rol>> listarRoles() {
        return ApiResponse.ok("Roles del sistema", rolRepository.findAll());
    }
}

