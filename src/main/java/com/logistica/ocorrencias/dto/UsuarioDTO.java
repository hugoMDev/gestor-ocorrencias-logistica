package com.logistica.ocorrencias.dto;

import com.logistica.ocorrencias.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioDTO {
    private UUID id;
    private String nome;
    private String username;
    private boolean ativo;
    private boolean admin;
    private LocalDateTime dataUltimaTrocaSenha;
    private Long diasDesdeTroca;
    private Long diasRestantes;

    public static UsuarioDTO fromEntity(Usuario usuario) {
        if (usuario == null) return null;
        Long dias = 0L;
        Long restantes = 15L;
        if (usuario.getDataUltimaTrocaSenha() != null) {
            dias = ChronoUnit.DAYS.between(usuario.getDataUltimaTrocaSenha(), LocalDateTime.now());
            restantes = Math.max(0, 15 - dias);
        }
        return UsuarioDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .username(usuario.getUsername())
                .ativo(usuario.isAtivo())
                .admin(usuario.isAdmin())
                .dataUltimaTrocaSenha(usuario.getDataUltimaTrocaSenha())
                .diasDesdeTroca(dias)
                .diasRestantes(restantes)
                .build();
    }
}
