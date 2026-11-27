package com.redcell.soat.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActualizarPerfilDTO {

    @Size(min = 4, max = 20, message = "El usuario debe tener entre 4 y 20 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]{4,20}$", message = "El usuario solo puede contener letras, números, punto, guion o guion bajo")
    private String username;

    @Email(message = "El formato del correo no es válido")
    @Size(max = 120, message = "El correo no puede superar 120 caracteres")
    private String email;
}