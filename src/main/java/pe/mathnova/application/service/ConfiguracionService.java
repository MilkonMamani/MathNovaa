package pe.mathnova.application.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.mathnova.infrastructure.persistence.ConfiguracionEntity;
import pe.mathnova.infrastructure.persistence.ConfiguracionRepository;

/**
 * Preferencias de la tabla configuracion:
 * <ul>
 *   <li>"animaciones": "true" o "false" (por defecto "true")</li>
 *   <li>"mostrar_codigos": "true" o "false" (por defecto "false"): si se ven los códigos internos
 *       de los métodos (F16, F04…) junto a su nombre</li>
 *   <li>"mostrar_explicaciones": "true" o "false" (por defecto "false"): si la Pizarra abre con todas
 *       las notas "por qué aplica" a la vista (el interruptor "Explicaciones")</li>
 * </ul>
 * Si la base falla, se usan los valores por defecto y la app sigue funcionando.
 */
@Service
public class ConfiguracionService {

    public static final String ANIMACIONES = "animaciones";
    public static final String MOSTRAR_CODIGOS = "mostrar_codigos";
    public static final String MOSTRAR_EXPLICACIONES = "mostrar_explicaciones";

    /** Claves que se crean al iniciar si faltan, con su valor por defecto. */
    private static final Map<String, String> POR_DEFECTO = Map.of(ANIMACIONES, "true", MOSTRAR_CODIGOS, "false",
            MOSTRAR_EXPLICACIONES, "false");

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguracionService.class);

    private final ConfiguracionRepository configuracion;

    public ConfiguracionService(ConfiguracionRepository configuracion) {
        this.configuracion = configuracion;
    }

    /** Al iniciar, crea las claves que todavía no existen (sin migración nueva). */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void crearClavesFaltantes() {
        POR_DEFECTO.forEach((clave, valor) -> {
            try {
                if (!configuracion.existsById(clave)) {
                    configuracion.save(new ConfiguracionEntity(clave, valor));
                }
            } catch (RuntimeException e) {
                LOG.warn("No se pudo crear la clave {} en configuracion", clave, e);
            }
        });
    }

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

    /** El interruptor "Explicaciones" de la Pizarra o de Configuración. Si la base falla, solo se avisa. */
    @Transactional
    public void guardarMostrarExplicaciones(boolean si) {
        guardar(MOSTRAR_EXPLICACIONES, si);
    }

    private void guardar(String clave, boolean valor) {
        try {
            configuracion.save(new ConfiguracionEntity(clave, String.valueOf(valor)));
        } catch (RuntimeException e) {
            LOG.warn("No se pudo guardar la clave {} en configuracion", clave, e);
        }
    }

    private boolean leer(String clave, boolean porDefecto) {
        try {
            return configuracion.findById(clave)
                    .map(c -> porDefecto ? !"false".equalsIgnoreCase(c.getValor().trim())
                            : "true".equalsIgnoreCase(c.getValor().trim()))
                    .orElse(porDefecto);
        } catch (RuntimeException e) {
            LOG.warn("No se pudo leer la clave {} de configuracion", clave, e);
            return porDefecto;
        }
    }
}
