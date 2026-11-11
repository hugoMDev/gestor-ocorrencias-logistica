package com.logistica.ocorrencias.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlacaValidatorTest {

    @Test
    @DisplayName("Deve validar placas válidas no padrão Mercosul")
    void deveValidarPlacasMercosul() {
        assertTrue(PlacaValidator.isValid("BRA2E19"));
        assertTrue(PlacaValidator.isValid("bra2e19")); // minúsculas
        assertTrue(PlacaValidator.isValid("ABC1D23"));
        assertTrue(PlacaValidator.isValid("XYZ9K88"));
        assertTrue(PlacaValidator.isMercosul("BRA2E19"));
    }

    @Test
    @DisplayName("Deve validar placas válidas no padrão Tradicional brasileiro")
    void deveValidarPlacasTradicionais() {
        assertTrue(PlacaValidator.isValid("ABC1234"));
        assertTrue(PlacaValidator.isValid("ABC-1234"));
        assertTrue(PlacaValidator.isValid("xyz-9876"));
        assertFalse(PlacaValidator.isMercosul("ABC1234"));
    }

    @Test
    @DisplayName("Deve rejeitar formatos de placa inválidos")
    void deveRejeitarPlacasInvalidas() {
        assertFalse(PlacaValidator.isValid(null));
        assertFalse(PlacaValidator.isValid(""));
        assertFalse(PlacaValidator.isValid("   "));
        assertFalse(PlacaValidator.isValid("1234ABC"));
        assertFalse(PlacaValidator.isValid("ABCD123"));
        assertFalse(PlacaValidator.isValid("ABC-12"));
        assertFalse(PlacaValidator.isValid("ABC12345"));
    }

    @Test
    @DisplayName("Deve normalizar placas removendo traços e convertendo para maiúsculas")
    void deveNormalizarPlacas() {
        assertEquals("ABC1234", PlacaValidator.normalizar("abc-1234"));
        assertEquals("BRA2E19", PlacaValidator.normalizar(" bra2e19 "));
    }

    @Test
    @DisplayName("Deve formatar placas tradicionais com hífen para exibição")
    void deveFormatarParaExibicao() {
        assertEquals("ABC-1234", PlacaValidator.formatarParaExibicao("ABC1234"));
        assertEquals("BRA2E19", PlacaValidator.formatarParaExibicao("BRA2E19"));
    }
}
