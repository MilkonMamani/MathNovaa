package pe.mathnova.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = "paso")
@Getter
@Setter
@NoArgsConstructor
public class PasoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resolucion_id", nullable = false)
    private ResolucionEntity resolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paso_padre_id")
    private PasoEntity padre;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "tecnica_codigo", nullable = false)
    private String tecnicaCodigo;

    @Column(name = "justificacion", nullable = false)
    private String justificacion;

    @Column(name = "antes_texto", nullable = false)
    private String antesTexto;

    @Column(name = "antes_latex", nullable = false)
    private String antesLatex;

    @Column(name = "despues_texto")
    private String despuesTexto;

    @Column(name = "despues_latex")
    private String despuesLatex;

    @Column(name = "trabajo_tipo")
    private String trabajoTipo;

    @Column(name = "trabajo_json")
    private String trabajoJson;

    @Convert(converter = BooleanoEnteroConverter.class)
    @Column(name = "es_descartado", nullable = false)
    private Boolean esDescartado = Boolean.FALSE;

    @Column(name = "motivo_descarte")
    private String motivoDescarte;
}
