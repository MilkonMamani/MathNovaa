package pe.mathnova.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sesion_practica")
@Getter
@Setter
@NoArgsConstructor
public class SesionPracticaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "resolucion_id", nullable = false)
    private Integer resolucionId;

    @Convert(converter = InstanteConverter.class)
    @Column(name = "iniciada_en", nullable = false)
    private Instant iniciadaEn;

    @Convert(converter = InstanteConverter.class)
    @Column(name = "terminada_en")
    private Instant terminadaEn;

    @Column(name = "total_pasos", nullable = false)
    private Integer totalPasos;

    @Column(name = "aciertos", nullable = false)
    private Integer aciertos = 0;
}
