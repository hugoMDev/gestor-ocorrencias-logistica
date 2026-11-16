package com.logistica.ocorrencias.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditarUsuarioRequest {
    private String nome;
    private String username;
    private Boolean admin;
    private String novaSenha;
}
