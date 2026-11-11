package com.logistica.ocorrencias.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Deserializador Jackson customizado para LocalDateTime com suporte a formatos com e sem segundos.
 */
public class JsonDateDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String texto = p.getText();
        return DateUtils.parse(texto);
    }
}
