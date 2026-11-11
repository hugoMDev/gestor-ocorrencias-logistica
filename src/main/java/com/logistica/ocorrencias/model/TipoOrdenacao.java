package com.logistica.ocorrencias.model;

/**
 * Critérios de ordenação para a listagem de ocorrências.
 */
public enum TipoOrdenacao {
    MAIS_RECENTES("Mais Recentes (Data/Hora Decrescente)"),
    MAIS_ANTIGOS("Mais Antigos (Data/Hora Crescente)");

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
