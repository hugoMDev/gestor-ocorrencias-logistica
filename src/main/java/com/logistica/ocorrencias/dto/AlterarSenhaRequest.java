package com.logistica.ocorrencias.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlterarSenhaRequest {
    private String username;
    private String senhaAtual;
    private String novaSenha;
}
