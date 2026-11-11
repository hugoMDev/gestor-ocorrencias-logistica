package com.logistica.ocorrencias.model;

import java.awt.Color;

/**
 * Níveis de gravidade para priorização das ocorrências logísticas.
 */
public enum Gravidade {
    BAIXA("Baixa", 1, new Color(74, 222, 128), new Color(22, 70, 40)),
    MEDIA("Média", 2, new Color(125, 211, 252), new Color(14, 65, 105)),
    ALTA("Alta", 3, new Color(251, 146, 60), new Color(124, 45, 18)),
    CRITICA("Crítica", 4, new Color(248, 113, 113), new Color(127, 29, 29));

    private final String descricao;
    private final int nivel;
    private final Color badgeForeground;
    private final Color badgeBackground;

    Gravidade(String descricao, int nivel, Color badgeForeground, Color badgeBackground) {
        this.descricao = descricao;
        this.nivel = nivel;
        this.badgeForeground = badgeForeground;
        this.badgeBackground = badgeBackground;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getNivel() {
        return nivel;
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
