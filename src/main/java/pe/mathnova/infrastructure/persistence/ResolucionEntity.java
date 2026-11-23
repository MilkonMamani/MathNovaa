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

/**
 * Tabla resolucion de V1. RN25: una sola fila por (ejercicio, operación, clave de parámetros).
 * tecnica_forzada (HU08, Sprint 3) todavía no se mapea; queda nula.
 */
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

    /** Q, R o C; solo para factorizar. */
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

    /** HU08: el método forzado, o null en automático. */
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

    @OneToMany(mappedBy = "resolucion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<PasoEntity> pasos = new ArrayList<>();

    /** HU10: las sugerencias que se mostraron con esta resolución. */
    @OneToMany(mappedBy = "resolucion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<SugerenciaEntity> sugerencias = new ArrayList<>();

    /** Tabla parametro_resolucion: variable, punto y lado de un límite. */
    @ElementCollection
    @CollectionTable(name = "parametro_resolucion", joinColumns = @JoinColumn(name = "resolucion_id"))
    @MapKeyColumn(name = "nombre")
    @Column(name = "valor", nullable = false)
    private Map<String, String> parametros = new LinkedHashMap<>();

    public void agregarPaso(PasoEntity paso) {
        paso.setResolucion(this);
        pasos.add(paso);
    }

    public void agregarSugerencia(SugerenciaEntity s) {
        s.setResolucion(this);
        sugerencias.add(s);
    }
}
