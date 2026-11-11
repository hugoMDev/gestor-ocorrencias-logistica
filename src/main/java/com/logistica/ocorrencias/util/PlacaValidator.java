package com.logistica.ocorrencias.util;

import java.util.regex.Pattern;

/**
 * Utilitário de validação e normalização de placas veiculares brasileiras (Padrão Mercosul e Tradicional).
 */
public final class PlacaValidator {

    // Padrão Mercosul: 3 letras, 1 número, 1 letra, 2 números (ex: BRA2E19, ABC1D23)
    private static final Pattern PADRAO_MERCOSUL = Pattern.compile("^[A-Z]{3}[0-9][A-Z][0-9]{2}$");

    // Padrão Tradicional (Cinza): 3 letras, traço opcional, 4 números (ex: ABC1234, ABC-1234)
    private static final Pattern PADRAO_TRADICIONAL = Pattern.compile("^[A-Z]{3}[0-9]{4}$");

    private PlacaValidator() {
        // Construtor privado para classe utilitária
    }

    /**
     * Remove caracteres especiais (espaços, traços) e converte para maiúsculas.
     */
    public static String normalizar(String placa) {
        if (placa == null) {
            return "";
        }
        return placa.trim().toUpperCase().replace("-", "").replace(" ", "");
    }

    /**
     * Formata visualmente a placa para exibição padronizada.
     * Exemplo: ABC1234 -> ABC-1234; BRA2E19 -> BRA2E19
     */
    public static String formatarParaExibicao(String placa) {
        String limpa = normalizar(placa);
        if (PADRAO_TRADICIONAL.matcher(limpa).matches()) {
            return limpa.substring(0, 3) + "-" + limpa.substring(3);
        }
        return limpa;
    }

    /**
     * Valida se a placa informada atende ao padrão Mercosul ou Tradicional brasileiro.
     */
    public static boolean isValid(String placa) {
        if (placa == null || placa.isBlank()) {
            return false;
        }
        String limpa = normalizar(placa);
        return PADRAO_MERCOSUL.matcher(limpa).matches() || PADRAO_TRADICIONAL.matcher(limpa).matches();
    }

    /**
     * Retorna se a placa é do formato Mercosul.
     */
    public static boolean isMercosul(String placa) {
        return PADRAO_MERCOSUL.matcher(normalizar(placa)).matches();
    }
}
