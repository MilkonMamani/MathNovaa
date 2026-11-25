package pe.mathnova.application.service;

import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.infrastructure.persistence.ConfiguracionEntity;
import pe.mathnova.infrastructure.persistence.ConfiguracionRepository;

/**
 * Preferencias de la tabla configuracion (clave, valor). Cada ajuste se guarda en el momento en que cambia.
 * <ul>
 *   <li>"campo_por_defecto": Q, R o C (por defecto Q, RN27)</li>
 *   <li>"modo_pizarra": "todos" o "paso" (cómo se abre la Pizarra; por defecto "todos")</li>
 *   <li>"mostrar_explicaciones": "true" o "false" (por defecto "false"): las notas de cada paso a la vista</li>
 *   <li>"mostrar_codigos": "true" o "false" (por defecto "false", RNF09): "F16 · Ruffini"</li>
 *   <li>"tamano_letra": "normal", "grande" o "muy_grande" (la Pizarra, la hoja del Historial, Práctica y el PDF)</li>
 *   <li>"tema_interfaz": "claro" u "oscuro" (en oscuro solo cambian fondo y texto; el azul se mantiene)</li>
 *   <li>"animaciones": "true" o "false" (por defecto "true")</li>
 * </ul>
 *
 * <p>Sprint 9, parte E: cada ajuste también es una propiedad observable que se lee una vez al arrancar; las
 * pantallas la escuchan y se actualizan sin reiniciar. Las propiedades se cambian en el hilo de la interfaz. Si la
 * base falla, se usan los valores por defecto y la app sigue funcionando.
 */
@Service
public class ConfiguracionService {

    public static final String ANIMACIONES = "animaciones";
    public static final String MOSTRAR_CODIGOS = "mostrar_codigos";
    public static final String MOSTRAR_EXPLICACIONES = "mostrar_explicaciones";
    public static final String CAMPO_POR_DEFECTO = "campo_por_defecto";
    public static final String MODO_PIZARRA = "modo_pizarra";
    public static final String TAMANO_LETRA = "tamano_letra";
    public static final String TEMA = "tema_interfaz";

    /** Cómo se abre la Pizarra con un resultado nuevo. */
    public enum ModoPizarra {
        TODOS("todos", "Todos los pasos"), PASO_A_PASO("paso", "Paso a paso");

        private final String valor;
        private final String texto;

        ModoPizarra(String valor, String texto) {
            this.valor = valor;
            this.texto = texto;
        }

        public String valor() {
            return valor;
        }

        public String texto() {
            return texto;
        }
    }

    /** El tamaño de la letra de la Pizarra, la hoja del Historial, Práctica y el PDF. */
    public enum TamanoLetra {
        NORMAL("normal", "Normal", 1.0), GRANDE("grande", "Grande", 1.2), MUY_GRANDE("muy_grande", "Muy grande", 1.4);

        private final String valor;
        private final String texto;
        private final double factor;

        TamanoLetra(String valor, String texto, double factor) {
            this.valor = valor;
            this.texto = texto;
            this.factor = factor;
        }

        public String valor() {
            return valor;
        }

        public String texto() {
            return texto;
        }

        /** Por cuánto se multiplica el tamaño normal. */
        public double factor() {
            return factor;
        }
    }

    public enum Tema {
        CLARO("claro", "Claro"), OSCURO("oscuro", "Oscuro");

        private final String valor;
        private final String texto;

        Tema(String valor, String texto) {
            this.valor = valor;
            this.texto = texto;
        }

        public String valor() {
            return valor;
        }

        public String texto() {
            return texto;
        }
    }

    /** Claves que se crean al iniciar si faltan, con su valor por defecto. */
    private static final Map<String, String> POR_DEFECTO = Map.of(ANIMACIONES, "true", MOSTRAR_CODIGOS, "false",
            MOSTRAR_EXPLICACIONES, "false", CAMPO_POR_DEFECTO, "Q", MODO_PIZARRA, ModoPizarra.TODOS.valor(),
            TAMANO_LETRA, TamanoLetra.NORMAL.valor(), TEMA, Tema.CLARO.valor());

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguracionService.class);

    private final ConfiguracionRepository configuracion;

    private final ObjectProperty<Campo> campo = new SimpleObjectProperty<>(this, "campo", Campo.Q);
    private final ObjectProperty<ModoPizarra> modo = new SimpleObjectProperty<>(this, "modo", ModoPizarra.TODOS);
    private final BooleanProperty explicaciones = new SimpleBooleanProperty(this, "explicaciones", false);
    private final BooleanProperty codigos = new SimpleBooleanProperty(this, "codigos", false);
    private final ObjectProperty<TamanoLetra> letra = new SimpleObjectProperty<>(this, "letra", TamanoLetra.NORMAL);
    private final ObjectProperty<Tema> tema = new SimpleObjectProperty<>(this, "tema", Tema.CLARO);
    private volatile boolean cargadas;

    public ConfiguracionService(ConfiguracionRepository configuracion) {
        this.configuracion = configuracion;
    }

    /** Al iniciar, crea las claves que todavía no existen (sin migración nueva). */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void crearClavesFaltantes() {
        if (configuracion == null) {
            return;
        }
        POR_DEFECTO.forEach((clave, valor) -> {
            try {
                if (!configuracion.existsById(clave)) {
                    configuracion.saveAndFlush(new ConfiguracionEntity(clave, valor));
                }
            } catch (RuntimeException e) {
                LOG.warn("No se pudo crear la clave {} en configuracion", clave, e);
            }
        });
    }

    // ------------------------------------------------------------------ lectura directa

    /** true salvo que la clave diga "false". */
    @Transactional(readOnly = true)
    public boolean animacionesActivas() {
        return leer(ANIMACIONES, true);
    }

    /** false salvo que la clave diga "true". */
    @Transactional(readOnly = true)
    public boolean mostrarCodigos() {
        return leer(MOSTRAR_CODIGOS, false);
    }

    /** false salvo que la clave diga "true". */
    @Transactional(readOnly = true)
    public boolean mostrarExplicaciones() {
        return leer(MOSTRAR_EXPLICACIONES, false);
    }

    /** Q salvo que la clave diga R o C. */
    @Transactional(readOnly = true)
    public Campo campoPorDefecto() {
        String v = leerTexto(CAMPO_POR_DEFECTO);
        return "R".equalsIgnoreCase(v) ? Campo.R : "C".equalsIgnoreCase(v) ? Campo.C : Campo.Q;
    }

    @Transactional(readOnly = true)
    public ModoPizarra modoPizarra() {
        return ModoPizarra.PASO_A_PASO.valor().equals(leerTexto(MODO_PIZARRA)) ? ModoPizarra.PASO_A_PASO
                : ModoPizarra.TODOS;
    }

    @Transactional(readOnly = true)
    public TamanoLetra tamanoLetra() {
        String v = leerTexto(TAMANO_LETRA);
        for (TamanoLetra t : TamanoLetra.values()) {
            if (t.valor().equals(v)) {
                return t;
            }
        }
        return TamanoLetra.NORMAL;
    }

    @Transactional(readOnly = true)
    public Tema tema() {
        return Tema.OSCURO.valor().equals(leerTexto(TEMA)) ? Tema.OSCURO : Tema.CLARO;
    }

    // ------------------------------------------------------------------ propiedades (sin reiniciar)

    /** Lee todo una vez, al pedir la primera propiedad. */
    private void cargar() {
        if (cargadas) {
            return;
        }
        synchronized (this) {
            if (cargadas) {
                return;
            }
            campo.set(campoPorDefecto());
            modo.set(modoPizarra());
            explicaciones.set(mostrarExplicaciones());
            codigos.set(mostrarCodigos());
            letra.set(tamanoLetra());
            tema.set(tema());
            cargadas = true;
        }
    }

    public ReadOnlyObjectProperty<Campo> campoProperty() {
        cargar();
        return campo;
    }

    public ReadOnlyObjectProperty<ModoPizarra> modoProperty() {
        cargar();
        return modo;
    }

    public ReadOnlyBooleanProperty explicacionesProperty() {
        cargar();
        return explicaciones;
    }

    public ReadOnlyBooleanProperty codigosProperty() {
        cargar();
        return codigos;
    }

    public ReadOnlyObjectProperty<TamanoLetra> letraProperty() {
        cargar();
        return letra;
    }

    public ReadOnlyObjectProperty<Tema> temaProperty() {
        cargar();
        return tema;
    }

    // ------------------------------------------------------------------ guardar

    @Transactional
    public void guardarCampoPorDefecto(Campo c) {
        cargar();
        Campo v = c == null ? Campo.Q : c;
        guardarTexto(CAMPO_POR_DEFECTO, v.name());
        campo.set(v);
    }

    @Transactional
    public void guardarModoPizarra(ModoPizarra m) {
        cargar();
        ModoPizarra v = m == null ? ModoPizarra.TODOS : m;
        guardarTexto(MODO_PIZARRA, v.valor());
        modo.set(v);
    }

    /** El interruptor "Explicaciones" de la Pizarra o de Configuración. Si la base falla, solo se avisa. */
    @Transactional
    public void guardarMostrarExplicaciones(boolean si) {
        cargar();
        guardar(MOSTRAR_EXPLICACIONES, si);
        explicaciones.set(si);
    }

    @Transactional
    public void guardarMostrarCodigos(boolean si) {
        cargar();
        guardar(MOSTRAR_CODIGOS, si);
        codigos.set(si);
    }

    @Transactional
    public void guardarTamanoLetra(TamanoLetra t) {
        cargar();
        TamanoLetra v = t == null ? TamanoLetra.NORMAL : t;
        guardarTexto(TAMANO_LETRA, v.valor());
        letra.set(v);
    }

    @Transactional
    public void guardarTema(Tema t) {
        cargar();
        Tema v = t == null ? Tema.CLARO : t;
        guardarTexto(TEMA, v.valor());
        tema.set(v);
    }

    // ------------------------------------------------------------------ base

    private void guardar(String clave, boolean valor) {
        guardarTexto(clave, String.valueOf(valor));
    }

    private void guardarTexto(String clave, String valor) {
        if (configuracion == null) {
            return;
        }
        try {
            configuracion.saveAndFlush(new ConfiguracionEntity(clave, valor));
        } catch (RuntimeException e) {
            LOG.warn("No se pudo guardar la clave {} en configuracion", clave, e);
        }
    }

    private boolean leer(String clave, boolean porDefecto) {
        String v = leerTexto(clave);
        if (v == null) {
            return porDefecto;
        }
        return porDefecto ? !"false".equalsIgnoreCase(v) : "true".equalsIgnoreCase(v);
    }

    /** El valor sin espacios; null si no está o si la base falla. */
    private String leerTexto(String clave) {
        if (configuracion == null) {
            return null;
        }
        try {
            return configuracion.findById(clave).map(c -> c.getValor().trim()).orElse(null);
        } catch (RuntimeException e) {
            LOG.warn("No se pudo leer la clave {} de configuracion", clave, e);
            return null;
        }
    }
}
