package com.logistica.ocorrencias.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade central representando um incidente/ocorrência operacional de transporte.
 */
public class Ocorrencia implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime dataHora;

    private PoloRegional poloRegional;
    private TipoOcorrencia tipoOcorrencia;
    private String placaVeiculo;
    private String motorista;
    private Gravidade gravidade;
    private StatusOcorrencia status;
    private String descricao;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime dataResolucao;

    private String observacaoResolucao;

    /**
     * Construtor padrão exigido pelo Jackson para deserialização JSON.
     */
    public Ocorrencia() {
    }

    /**
     * Construtor completo para novas ocorrências.
     */
    public Ocorrencia(String id, LocalDateTime dataHora, PoloRegional poloRegional,
                      TipoOcorrencia tipoOcorrencia, String placaVeiculo, String motorista,
                      Gravidade gravidade, StatusOcorrencia status, String descricao) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.dataHora = (dataHora != null) ? dataHora : LocalDateTime.now();
        this.poloRegional = poloRegional;
        this.tipoOcorrencia = tipoOcorrencia;
        this.placaVeiculo = placaVeiculo;
        this.motorista = motorista;
        this.gravidade = gravidade;
        this.status = (status != null) ? status : StatusOcorrencia.PENDENTE;
        this.descricao = descricao;
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public PoloRegional getPoloRegional() {
        return poloRegional;
    }

    public void setPoloRegional(PoloRegional poloRegional) {
        this.poloRegional = poloRegional;
    }

    public TipoOcorrencia getTipoOcorrencia() {
        return tipoOcorrencia;
    }

    public void setTipoOcorrencia(TipoOcorrencia tipoOcorrencia) {
        this.tipoOcorrencia = tipoOcorrencia;
    }

    public String getPlacaVeiculo() {
        return placaVeiculo;
    }

    public void setPlacaVeiculo(String placaVeiculo) {
        this.placaVeiculo = placaVeiculo;
    }

    public String getMotorista() {
        return motorista;
    }

    public void setMotorista(String motorista) {
        this.motorista = motorista;
    }

    public Gravidade getGravidade() {
        return gravidade;
    }

    public void setGravidade(Gravidade gravidade) {
        this.gravidade = gravidade;
    }

    public StatusOcorrencia getStatus() {
        return status;
    }

    public void setStatus(StatusOcorrencia status) {
        this.status = status;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDateTime getDataResolucao() {
        return dataResolucao;
    }

    public void setDataResolucao(LocalDateTime dataResolucao) {
        this.dataResolucao = dataResolucao;
    }

    public String getObservacaoResolucao() {
        return observacaoResolucao;
    }

    public void setObservacaoResolucao(String observacaoResolucao) {
        this.observacaoResolucao = observacaoResolucao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ocorrencia that = (Ocorrencia) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Ocorrencia{" +
                "id='" + id + '\'' +
                ", dataHora=" + dataHora +
                ", poloRegional=" + poloRegional +
                ", tipoOcorrencia=" + tipoOcorrencia +
                ", placaVeiculo='" + placaVeiculo + '\'' +
                ", motorista='" + motorista + '\'' +
                ", gravidade=" + gravidade +
                ", status=" + status +
                '}';
    }
}
