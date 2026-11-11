package com.logistica.ocorrencias.model;

import java.awt.Color;

/**
 * Status do fluxo de tratamento da ocorrência logística.
 */
public enum StatusOcorrencia {
    PENDENTE("Pendente", new Color(251, 191, 36), new Color(120, 53, 15)),
    EM_ANALISE("Em Análise", new Color(56, 189, 248), new Color(12, 74, 110)),
    RESOLVIDO("Resolvido", new Color(52, 211, 153), new Color(6, 78, 59));

    private final String descricao;
    private final Color badgeForeground;
    private final Color badgeBackground;

    StatusOcorrencia(String descricao, Color badgeForeground, Color badgeBackground) {
        this.descricao = descricao;
        this.badgeForeground = badgeForeground;
        this.badgeBackground = badgeBackground;
    }

    public String getDescricao() {
        return descricao;
    }

    public Color getBadgeForeground() {
        return badgeForeground;
    }

    public Color getBadgeBackground() {
        return badgeBackground;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
