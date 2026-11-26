package com.redcell.soat.services;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.redcell.soat.dto.RegisterRequestDTO;
import com.redcell.soat.entity.User;
import com.redcell.soat.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    // Generador seguro para los códigos OTP
    private static final SecureRandom secureRandom = new SecureRandom();

    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!user.getEstado()) throw new RuntimeException("Usuario inactivo");

        // Bloqueo por 3 intentos
        if (user.getFailedAttempts() >= 3) {
            long secondsRemaining = Duration.between(LocalDateTime.now(), user.getLockTime().plusSeconds(30)).getSeconds();
            if (secondsRemaining > 0) throw new RuntimeException("Cuenta bloqueada. Intente en " + secondsRemaining + " segundos");
            user.setFailedAttempts(0);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            if (user.getFailedAttempts() >= 3) user.setLockTime(LocalDateTime.now());
            userRepository.save(user);
            throw new RuntimeException("Credenciales incorrectas. Intentos restantes: " + (3 - user.getFailedAttempts()));
        }

        user.setFailedAttempts(0);
        userRepository.save(user);

        return jwtService.generateToken(user.getId(), user.getUsername(), user.getRol());
    }

    @Transactional 
    public void register(RegisterRequestDTO request) {
        if (userRepository.findByEmailOrUsername(request.getEmail(), request.getUsername()).isPresent()) {
            throw new RuntimeException("El email o nombre de usuario ya está en uso");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRol(request.getRol());
        user.setEstado(true);
        
        userRepository.save(user);
    }

    // --- LÓGICA DE OLVIDO DE CONTRASEÑA (OTP) ---
    @Transactional
    public void solicitarRecuperacion(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("El correo no se encuentra registrado en el sistema"));

        // Generar token OTP de 6 dígitos de forma segura
        int numero = secureRandom.nextInt(900000) + 100000; // Genera un número entre 100000 y 999999
        String token = String.valueOf(numero);
        
        user.setResetToken(token);
        user.setResetTokenExpiration(LocalDateTime.now().plusMinutes(15)); // Expira en 15 mins
        userRepository.save(user);

        emailService.enviarCorreoRecuperacion(user.getEmail(), user.getUsername(), token);
    }

    @Transactional
    public void cambiarContrasenaConToken(String token, String nuevaContrasena) {
        // Buscar directamente en la BD en lugar de traer todos los usuarios
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Código OTP inválido"));

        if (user.getResetTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El código OTP ha expirado. Solicita uno nuevo.");
        }

        if (passwordEncoder.matches(nuevaContrasena, user.getPassword())) {
            throw new RuntimeException("La nueva contraseña no puede ser igual a la anterior");
        }

        // Actualizar contraseña y limpiar el token
        user.setPassword(passwordEncoder.encode(nuevaContrasena));
        user.setResetToken(null); // Se quema el token para que no se vuelva a usar
        user.setResetTokenExpiration(null);
        
        // Si estaba bloqueado por intentos fallidos, lo desbloqueamos al cambiar la clave
        user.setFailedAttempts(0);
        user.setLockTime(null);
        
        userRepository.save(user);
    }
}