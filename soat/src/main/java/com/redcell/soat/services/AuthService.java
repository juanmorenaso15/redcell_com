package com.redcell.soat.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.redcell.soat.dto.ActualizarPerfilDTO;
import com.redcell.soat.dto.PerfilResponseDTO;
import com.redcell.soat.dto.RegisterRequestDTO;
import com.redcell.soat.entity.User;
import com.redcell.soat.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_INTENTOS = 3;
    private static final long BLOQUEO_SEGUNDOS = 60L;

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final SecureRandom secureRandom = new SecureRandom();

    public String login(String email, String password) {
        String emailNormalizado = email != null ? email.trim().toLowerCase() : "";
        User user = userRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        if (!Boolean.TRUE.equals(user.getEstado())) {
            throw new RuntimeException("Usuario inactivo o suspendido");
        }

        int intentosActuales = user.getFailedAttempts() != null ? user.getFailedAttempts() : 0;
        if (intentosActuales >= MAX_INTENTOS) {
            if (user.getLockTime() == null) {
                user.setLockTime(LocalDateTime.now());
            }
            long secondsRemaining = Duration
                    .between(LocalDateTime.now(), user.getLockTime().plusSeconds(BLOQUEO_SEGUNDOS)).getSeconds();
            if (secondsRemaining > 0) {
                userRepository.save(user);
                throw new RuntimeException(
                        "Cuenta bloqueada temporalmente. Intente en " + secondsRemaining + " segundos");
            }
            user.setFailedAttempts(0);
            user.setLockTime(null);
            intentosActuales = 0;
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            int nuevosIntentos = intentosActuales + 1;
            user.setFailedAttempts(nuevosIntentos);
            if (nuevosIntentos >= MAX_INTENTOS) {
                user.setLockTime(LocalDateTime.now());
            }
            userRepository.save(user);
            throw new RuntimeException("Credenciales incorrectas");
        }

        user.setFailedAttempts(0);
        user.setLockTime(null);
        userRepository.save(user);

        return jwtService.generateToken(user.getId(), user.getUsername(), user.getRol());
    }

    @Transactional
    public void register(RegisterRequestDTO request) {
        String emailLimpio = request.getEmail().trim().toLowerCase();
        String usernameLimpio = request.getUsername().trim();
        String rolLimpio = request.getRol().trim().toUpperCase();

        if (userRepository.findByEmailOrUsername(emailLimpio, usernameLimpio).isPresent()) {
            throw new RuntimeException("El email o nombre de usuario ya está en uso");
        }

        User user = new User();
        user.setEmail(emailLimpio);
        user.setUsername(usernameLimpio);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRol(rolLimpio);
        user.setEstado(true);
        user.setFailedAttempts(0);
        user.setLockTime(null);

        userRepository.save(user);
    }

    public List<PerfilResponseDTO> listarUsuarios() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(u -> new PerfilResponseDTO(u.getId(), u.getEmail(), u.getUsername(), u.getRol()))
                .toList();
    }

    @Transactional
    public void solicitarRecuperacion(String email) {
        String emailLimpio = email != null ? email.trim().toLowerCase() : "";
        userRepository.findByEmail(emailLimpio).ifPresent(user -> {
            if (!Boolean.TRUE.equals(user.getEstado())) {
                return;
            }
            int numero = secureRandom.nextInt(900000) + 100000;
            String codigoOtp = String.valueOf(numero);

            user.setResetToken(hashOtp(codigoOtp));
            user.setResetTokenExpiration(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);

            emailService.enviarCorreoRecuperacion(user.getEmail(), user.getUsername(), codigoOtp);
        });
    }

    @Transactional
    public void cambiarContrasenaConToken(String token, String nuevaContrasena) {
        String tokenLimpio = token != null ? token.trim() : "";
        String hashBuscado = hashOtp(tokenLimpio);

        User user = userRepository.findByResetToken(hashBuscado)
                .orElseGet(() -> userRepository.findByResetToken(tokenLimpio)
                        .orElseThrow(() -> new RuntimeException("Código OTP inválido")));

        if (user.getResetTokenExpiration() == null || user.getResetTokenExpiration().isBefore(LocalDateTime.now())) {
            user.setResetToken(null);
            user.setResetTokenExpiration(null);
            userRepository.save(user);
            throw new RuntimeException("El código OTP ha expirado. Solicita uno nuevo.");
        }

        if (passwordEncoder.matches(nuevaContrasena, user.getPassword())) {
            throw new RuntimeException("La nueva contraseña no puede ser igual a la anterior");
        }

        user.setPassword(passwordEncoder.encode(nuevaContrasena));
        user.setResetToken(null);
        user.setResetTokenExpiration(null);
        user.setFailedAttempts(0);
        user.setLockTime(null);

        userRepository.save(user);
    }

    public PerfilResponseDTO obtenerPerfil(String emailActual) {
        User user = buscarUsuarioActual(emailActual);
        return new PerfilResponseDTO(user.getId(), user.getEmail(), user.getUsername(), user.getRol());
    }

    @Transactional
    public PerfilResponseDTO actualizarPerfil(String emailActual, ActualizarPerfilDTO dto) {
        User user = buscarUsuarioActual(emailActual);

        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            String nuevoUser = dto.getUsername().trim();
            if (!nuevoUser.equals(user.getUsername()) && userRepository.findByUsername(nuevoUser).isPresent()) {
                throw new RuntimeException("Ese nombre de usuario ya está en uso");
            }
            user.setUsername(nuevoUser);
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String nuevoEmail = dto.getEmail().trim().toLowerCase();
            if (!nuevoEmail.equalsIgnoreCase(user.getEmail()) && userRepository.findByEmail(nuevoEmail).isPresent()) {
                throw new RuntimeException("Ese correo electrónico ya está registrado");
            }
            user.setEmail(nuevoEmail);
        }

        User guardado = userRepository.save(user);
        return new PerfilResponseDTO(guardado.getId(), guardado.getEmail(), guardado.getUsername(), guardado.getRol());
    }

    private User buscarUsuarioActual(String principal) {
        if (principal == null || principal.isBlank()) {
            throw new RuntimeException("Sesión no válida");
        }
        return userRepository.findByEmail(principal.trim().toLowerCase())
                .or(() -> userRepository.findByUsername(principal.trim()))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encoded);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo inicializar SHA-256", e);
        }
    }
}