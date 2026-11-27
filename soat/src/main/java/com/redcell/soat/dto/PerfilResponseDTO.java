package com.redcell.soat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data 
@AllArgsConstructor 
public class PerfilResponseDTO {
    private Long id;
    private String email;
    private String username;
    private String rol;
    private Boolean estado;
}