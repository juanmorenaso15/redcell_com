package com.redcell.soat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.redcell.soat.dto.ActualizarPerfilDTO;
import com.redcell.soat.dto.ApiResponse;
import com.redcell.soat.dto.AuthResponse;
import com.redcell.soat.dto.ForgotPasswordDTO;
import com.redcell.soat.dto.LoginRequestDTO;
import com.redcell.soat.dto.PerfilResponseDTO;
import com.redcell.soat.dto.RegisterRequestDTO;
import com.redcell.soat.dto.ResetPasswordDTO;
import com.redcell.soat.services.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequestDTO request) {
        String token = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequestDTO request) {
        authService.register(request);
        return ResponseEntity.ok(new ApiResponse("Usuario registrado exitosamente"));
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PerfilResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(authService.listarUsuarios());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordDTO request) {
        authService.solicitarRecuperacion(request.getEmail());
        return ResponseEntity.ok(new ApiResponse("Si el correo está registrado, recibirás un código."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordDTO request) {
        authService.cambiarContrasenaConToken(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(new ApiResponse("Contraseña actualizada exitosamente"));
    }

    @GetMapping("/perfil")
    public ResponseEntity<PerfilResponseDTO> obtenerPerfil(Authentication auth) {
        return ResponseEntity.ok(authService.obtenerPerfil(auth.getName()));
    }

    @PutMapping("/perfil")
    public ResponseEntity<PerfilResponseDTO> actualizarPerfil(
            Authentication auth,
            @Valid @RequestBody ActualizarPerfilDTO dto) {
        return ResponseEntity.ok(authService.actualizarPerfil(auth.getName(), dto));
    }

    @PatchMapping("/usuarios/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PerfilResponseDTO> cambiarEstadoUsuario(
            @PathVariable Long id,
            @RequestParam boolean activo,
            Authentication auth) {
        PerfilResponseDTO actualizado = authService.cambiarEstadoUsuario(id, activo, auth.getName());
        return ResponseEntity.ok(actualizado);
    }
}