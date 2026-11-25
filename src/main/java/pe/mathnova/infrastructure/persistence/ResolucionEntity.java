package pe.mathnova.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resolucion")
@Getter
@Setter
@NoArgsConstructor
public class ResolucionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejercicio_id", nullable = false)
    private EjercicioEntity ejercicio;

    @Column(name = "operacion_codigo", nullable = false)
    private String operacionCodigo;

    @Column(name = "campo")
    private String campo;

    @Column(name = "clave_parametros", nullable = false)
    private String claveParametros;

    @Column(name = "resultado_texto", nullable = false)
    private String resultadoTexto;

    @Column(name = "resultado_latex", nullable = false)
    private String resultadoLatex;

    @Convert(converter = BooleanoEnteroConverter.class)
    @Column(name = "es_aproximado", nullable = false)
    private Boolean esAproximado = Boolean.FALSE;

    @Column(name = "tecnica_forzada")
    private String tecnicaForzada;

    @Column(name = "version_motor", nullable = false)
    private String versionMotor;

    @Convert(converter = InstanteConverter.class)
    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Convert(converter = InstanteConverter.class)
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Column(name = "entrada_texto")
    private String entradaTexto;

    @Column(name = "entrada_latex")
    private String entradaLatex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tema_id")
    private TemaEntity tema;

    @OneToMany(mappedBy = "resolucion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<PasoEntity> pasos = new ArrayList<>();

    @OneToMany(mappedBy = "resolucion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<SugerenciaEntity> sugerencias = new ArrayList<>();

    @OneToMany(mappedBy = "resolucion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<RestriccionDominioEntity> restricciones = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "parametro_resolucion", joinColumns = @JoinColumn(name = "resolucion_id"))
    @MapKeyColumn(name = "nombre")
    @Column(name = "valor", nullable = false)
    private Map<String, String> parametros = new LinkedHashMap<>();

    public void agregarPaso(PasoEntity paso) {
        paso.setResolucion(this);
        pasos.add(paso);
    }

    public void agregarRestriccion(RestriccionDominioEntity r) {
        r.setResolucion(this);
        restricciones.add(r);
    }

    public void agregarSugerencia(SugerenciaEntity s) {
        s.setResolucion(this);
        sugerencias.add(s);
    }
}
