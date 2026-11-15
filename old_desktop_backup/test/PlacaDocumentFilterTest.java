package com.logistica.ocorrencias.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;

import static org.junit.jupiter.api.Assertions.*;

class PlacaDocumentFilterTest {

    @Test
    @DisplayName("Deve formatar placa tradicional adicionando hífen automaticamente ao digitar 5º caractere numérico")
    void deveFormatarPlacaTradicional() {
        assertEquals("ABC-1234", PlacaDocumentFilter.formatarPlacaDinamica("abc1234"));
        assertEquals("ABC-12", PlacaDocumentFilter.formatarPlacaDinamica("abc12"));
        assertEquals("XYZ-9876", PlacaDocumentFilter.formatarPlacaDinamica("xyz-9876"));
    }

    @Test
    @DisplayName("Deve manter placa Mercosul sem hífen quando o 5º caractere for letra")
    void deveFormatarPlacaMercosulSemHifen() {
        assertEquals("BRA2E19", PlacaDocumentFilter.formatarPlacaDinamica("bra2e19"));
        assertEquals("ABC1D23", PlacaDocumentFilter.formatarPlacaDinamica("abc1d23"));
        assertEquals("ABC1D", PlacaDocumentFilter.formatarPlacaDinamica("abc1d"));
    }

    @Test
    @DisplayName("Deve permitir digitação contínua sem quebrar formato em JTextField Swing")
    void deveFuncionarEmComponenteSwing() throws BadLocationException {
        JTextField textField = new JTextField();
        ((AbstractDocument) textField.getDocument()).setDocumentFilter(new PlacaDocumentFilter());

        // Simula digitação tradicional
        textField.setText("abc1234");
        assertEquals("ABC-1234", textField.getText());

        // Simula digitação Mercosul
        textField.setText("bra2e19");
        assertEquals("BRA2E19", textField.getText());

        // Limpeza
        textField.setText("");
        assertEquals("", textField.getText());
    }
}
