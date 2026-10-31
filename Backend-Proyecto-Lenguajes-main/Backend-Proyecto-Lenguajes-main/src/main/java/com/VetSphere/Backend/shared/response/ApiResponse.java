package com.VetSphere.Backend.shared.response;

public record ApiResponse<T>(boolean ok, String mensaje, T data) {
    public static <T> ApiResponse<T> ok(String mensaje, T data) {
        return new ApiResponse<>(true, mensaje, data);
    }

    public static <T> ApiResponse<T> error(String mensaje) {
        return new ApiResponse<>(false, mensaje, null);
    }
}


