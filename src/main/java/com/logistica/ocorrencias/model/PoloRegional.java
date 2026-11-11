package com.logistica.ocorrencias.model;

/**
 * Polos operacionais e regionais monitorados pela logística.
 */
public enum PoloRegional {
    ITUPEVA("Itupeva - SP (CD Central)"),
    PAULINIA("Paulínia - SP (Hub Petroquímico)"),
    SAO_JOSE_DOS_CAMPOS("São José dos Campos - SP (Vale do Paraíba)"),
    CAPITAL_SP("Capital - SP (Centro de Distribuição Urbana)"),
    OUTRO("Outro / Rota Interestadual");

    private final String descricao;

    PoloRegional(String descricao) {
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
