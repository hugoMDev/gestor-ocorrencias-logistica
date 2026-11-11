package com.logistica.ocorrencias.service;

/**
 * Exceção disparada quando uma regra de validação de negócio de ocorrência é violada.
 */
public class ValidacaoException extends RuntimeException {

    public ValidacaoException(String message) {
        super(message);
    }
}
