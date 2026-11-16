package com.logistica.ocorrencias.service;

import com.logistica.ocorrencias.model.Usuario;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthTokenService {

    private final Map<String, UsuarioSession> sessoesAtivas = new ConcurrentHashMap<>();

    public String criarSessao(Usuario usuario) {
        String token = UUID.randomUUID().toString();
        UsuarioSession session = UsuarioSession.builder()
                .token(token)
                .userId(usuario.getId())
                .username(usuario.getUsername())
                .nome(usuario.getNome())
                .admin(usuario.isAdmin())
                .criadoEm(LocalDateTime.now())
                .build();

        sessoesAtivas.put(token, session);
        return token;
    }

    public Optional<UsuarioSession> validarToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessoesAtivas.get(token.trim()));
    }

    public void removerSessao(String token) {
        if (token != null) {
            sessoesAtivas.remove(token.trim());
        }
    }

    public int totalSessoesAtivas() {
        return sessoesAtivas.size();
    }
}
