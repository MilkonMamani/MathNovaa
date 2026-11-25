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

/** Tabla intento_paso de V1 (Sprint 9, parte D): cada opción que el alumno eligió en una práctica. */
@Entity
@Table(name = "intento_paso")
@Getter
@Setter
@NoArgsConstructor
public class IntentoPasoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "sesion_id", nullable = false)
    private Integer sesionId;

    /** 1, 2, 3…: la pregunta de la práctica (la última es "¿Terminaste?" en una factorización). */
    @Column(name = "orden_paso", nullable = false)
    private Integer ordenPaso;

    /** La técnica elegida; null si la opción no es una técnica del catálogo ("Sí, todos los factores…"). */
    @Column(name = "tecnica_propuesta")
    private String tecnicaPropuesta;

    /** El texto de la opción elegida. */
    @Column(name = "expresion_propuesta", nullable = false)
    private String expresionPropuesta;

    @Convert(converter = BooleanoEnteroConverter.class)
    @Column(name = "es_correcto", nullable = false)
    private Boolean esCorrecto;

    /** "Correcto" o el motivo por el que no aplica. */
    @Column(name = "retroalimentacion", nullable = false)
    private String retroalimentacion;

    @Convert(converter = InstanteConverter.class)
    @Column(name = "intentado_en", nullable = false)
    private Instant intentadoEn;
}
