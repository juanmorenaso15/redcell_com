package com.redcell.soat.dto;

import lombok.Data;

@Data
public class NotificacionRequest {
    private String telefono;
    private String email;
    private Boolean whatsapp;
    private Boolean gmail;
}