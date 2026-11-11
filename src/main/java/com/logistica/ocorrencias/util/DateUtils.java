package com.logistica.ocorrencias.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

/**
 * Utilitários para formatação e conversão de datas no padrão brasileiro com e sem segundos.
 */
public final class DateUtils {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final DateTimeFormatter STORAGE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static final DateTimeFormatter FLEXIBLE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("dd/MM/yyyy HH:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalEnd()
            .toFormatter();

    private DateUtils() {
    }

    public static String formatar(LocalDateTime dataHora) {
        if (dataHora == null) {
            return "-";
        }
        return dataHora.format(FORMATTER);
    }

    public static String formatarComSegundos(LocalDateTime dataHora) {
        if (dataHora == null) {
            return null;
        }
        return dataHora.format(STORAGE_FORMATTER);
    }

    public static LocalDateTime parse(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(texto.trim(), FLEXIBLE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
