package com.logistica.ocorrencias.model;

/**
 * Categorias de incidentes operacionais de transporte.
 */
public enum TipoOcorrencia {
    AVARIA("Avaria na Carga"),
    DESVIO_ROTA("Desvio de Rota / Telemetria"),
    ATRASO_CARGA("Atraso no Carregamento / Descarga"),
    SINISTRO("Sinistro / Acidente na Via"),
    FALHA_VEICULO("Falha Mecânica / Veículo Parado");

    private final String descricao;

    TipoOcorrencia(String descricao) {
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
