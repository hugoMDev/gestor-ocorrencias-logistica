package com.logistica.ocorrencias.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NovoUsuarioRequest {
    private String nome;
    private String username;
    private String senha;
    private boolean admin;
}
