package com.logistica.ocorrencias.model;

/**
 * Critérios de ordenação para a listagem de ocorrências.
 */
public enum TipoOrdenacao {
    MAIS_RECENTES("Mais Recentes (Data/Hora Decrescente)"),
    MAIS_ANTIGAS("Mais Antigas (Data/Hora Crescente)"),
    GRAVIDADE_DECRESCENTE("Maior Gravidade (Crítica Primeiro)"),
    GRAVIDADE_CRESCENTE("Menor Gravidade (Baixa Primeiro)");

    private final String descricao;

    TipoOrdenacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
