package pe.mathnova.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tabla ejemplo de V1, con nota y orden de V4 (Sprint 9). Catálogo de solo lectura: lo llena la migración y
 * EjemplosTest comprueba que el motor resuelve cada fila (RN20).
 */
@Entity
@Table(name = "ejemplo")
@Getter
@NoArgsConstructor
public class EjemploEntity {

    @Id
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tema_id", nullable = false)
    private TemaEntity tema;

    @Column(name = "enunciado", nullable = false)
    private String enunciado;

    /** El texto de la sección 9 que se pone en el editor. */
    @Column(name = "expresion", nullable = false)
    private String expresion;

    @Column(name = "operacion_codigo", nullable = false)
    private String operacionCodigo;

    @Column(name = "parametros_json")
    private String parametrosJson;

    @Column(name = "resultado_esperado", nullable = false)
    private String resultadoEsperado;

    @Column(name = "dificultad", nullable = false)
    private Integer dificultad;

    @Column(name = "nota")
    private String nota;

    @Column(name = "orden", nullable = false)
    private Integer orden;
}
