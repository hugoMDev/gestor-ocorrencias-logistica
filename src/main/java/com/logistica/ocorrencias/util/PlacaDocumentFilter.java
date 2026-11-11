package com.logistica.ocorrencias.util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

/**
 * Filtro de documento Swing para formatação dinâmica e conversão automática de placas para caixa alta.
 * - Formato Tradicional: Adiciona hífen automaticamente quando o 5º caractere for numérico (ex: ABC-1234).
 * - Formato Mercosul: Mantém sem hífen quando o 5º caractere for uma letra (ex: BRA2E19).
 */
public class PlacaDocumentFilter extends DocumentFilter {

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        if (string == null) return;
        replace(fb, offset, 0, string, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        if (text == null) {
            fb.replace(offset, length, null, attrs);
            return;
        }

        Document doc = fb.getDocument();
        String currentText = doc.getText(0, doc.getLength());
        StringBuilder sb = new StringBuilder(currentText);
        sb.replace(offset, offset + length, text);

        String formatted = formatarPlacaDinamica(sb.toString());
        if (formatted.length() > 8) {
            formatted = formatted.substring(0, 8);
        }

        fb.replace(0, doc.getLength(), formatted, attrs);
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        Document doc = fb.getDocument();
        String currentText = doc.getText(0, doc.getLength());

        // Se estiver apagando o hífen isolado, apaga também o caractere anterior para não travar a digitação
        if (length == 1 && offset < currentText.length() && currentText.charAt(offset) == '-') {
            if (offset > 0) {
                offset--;
                length = 2;
            }
        }

        StringBuilder sb = new StringBuilder(currentText);
        sb.delete(offset, offset + length);

        String formatted = formatarPlacaDinamica(sb.toString());
        fb.replace(0, doc.getLength(), formatted, null);
    }

    /**
     * Aplica a regra de formatação dinâmica com base no conteúdo digitado.
     */
    public static String formatarPlacaDinamica(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // Mantém apenas letras e números e converte para maiúsculo
        String raw = input.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (raw.isEmpty()) {
            return "";
        }

        // Limita ao máximo de 7 caracteres alfanuméricos
        if (raw.length() > 7) {
            raw = raw.substring(0, 7);
        }

        // Com até 4 caracteres, não adiciona hífen
        if (raw.length() <= 4) {
            return raw;
        }

        // No 5º caractere (índice 4):
        // Se for letra, trata como padrão Mercosul (ex: BRA2E19) -> Sem hífen
        char quintoChar = raw.charAt(4);
        if (Character.isLetter(quintoChar)) {
            return raw;
        }

        // Se for número, trata como padrão Tradicional (ex: ABC-1234) -> Adiciona hífen
        String letras = raw.substring(0, Math.min(3, raw.length()));
        String resto = raw.length() > 3 ? raw.substring(3) : "";
        return letras + "-" + resto;
    }
}
