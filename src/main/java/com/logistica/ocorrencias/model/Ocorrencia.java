package com.logistica.ocorrencias.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ocorrencias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ocorrencia {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    @JsonAlias({"codigo", "codigo_oco"})
    private String codigoOco = "OCO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    @Column
    @JsonAlias({"data_hora"})
    private LocalDateTime dataHora = LocalDateTime.now();

    @Column
    @JsonAlias({"polo_regional"})
    private String poloRegional;

    @Column
    @JsonAlias({"tipo_ocorrencia"})
    private String tipoOcorrencia;

    @Column(length = 10)
    @JsonAlias({"placa", "placa_veiculo"})
    private String placaVeiculo;

    @Column
    private String motorista;

    @Column
    private String gravidade = "MEDIA";

    @Column
    private String status = "PENDENTE";

    @Column
    @JsonAlias({"monitor", "monitor_responsavel"})
    private String monitorResponsavel = "Operador Central";

    @Column(columnDefinition = "TEXT")
    @JsonAlias({"descricao", "descricao_fatos"})
    private String descricaoFatos;

    @PrePersist
    public void prePersist() {
        if (this.codigoOco == null || this.codigoOco.isBlank()) {
            this.codigoOco = "OCO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (this.dataHora == null) {
            this.dataHora = LocalDateTime.now();
        }
        if (this.status == null || this.status.isBlank()) {
            this.status = "PENDENTE";
        }
        if (this.gravidade == null || this.gravidade.isBlank()) {
            this.gravidade = "MEDIA";
        }
        if (this.monitorResponsavel == null || this.monitorResponsavel.isBlank()) {
            this.monitorResponsavel = "Operador Central";
        }
    }
}
