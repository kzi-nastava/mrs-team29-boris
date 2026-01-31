package com.example.backendspringboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserResponseDTO {

    private Long id;
    private String username;
    private String role;
}