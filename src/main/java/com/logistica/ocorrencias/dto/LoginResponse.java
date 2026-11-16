package com.logistica.ocorrencias.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String status;
    private String mensagem;
    private String token;
    private UsuarioDTO usuario;
    private boolean avisoExpiracao;
    private Long diasRestantes;
}
