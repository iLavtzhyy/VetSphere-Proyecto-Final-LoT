package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.dto.DashboardResponse;
import com.VetSphere.Backend.shared.response.ApiResponse;
import com.VetSphere.Backend.modules.clinic.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    public ApiResponse<DashboardResponse> resumen() {
        return ApiResponse.ok("Resumen clinico", dashboardService.resumen());
    }
}


