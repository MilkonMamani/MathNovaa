package pe.mathnova.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sugerencia")
@Getter
@Setter
@NoArgsConstructor
public class SugerenciaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resolucion_id", nullable = false)
    private ResolucionEntity resolucion;

    @Column(name = "operacion_codigo", nullable = false)
    private String operacionCodigo;

    @Column(name = "regla_codigo", nullable = false)
    private String reglaCodigo;

    @Column(name = "justificacion", nullable = false)
    private String justificacion;

    @Column(name = "parametros_json")
    private String parametrosJson;

    @Column(name = "orden", nullable = false)
    private Integer orden;
}
