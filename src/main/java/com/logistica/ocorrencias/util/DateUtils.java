package com.logistica.ocorrencias.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utilitários para formatação e conversão de datas no padrão brasileiro.
 */
public final class DateUtils {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DateUtils() {
    }

    public static String formatar(LocalDateTime dataHora) {
        if (dataHora == null) {
            return "-";
        }
        return dataHora.format(FORMATTER);
    }

    public static LocalDateTime parse(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(texto.trim(), FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
