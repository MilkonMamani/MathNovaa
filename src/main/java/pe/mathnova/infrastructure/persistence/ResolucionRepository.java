package pe.mathnova.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResolucionRepository extends JpaRepository<ResolucionEntity, Integer>,
        JpaSpecificationExecutor<ResolucionEntity> {

    /** RN25: la resolución de un ejercicio con una operación y una clave de parámetros. */
    Optional<ResolucionEntity> findByEjercicioAndOperacionCodigoAndClaveParametros(
            EjercicioEntity ejercicio, String operacionCodigo, String claveParametros);

    /** La tabla del Historial: una página con el ejercicio y el tema de cada fila, sin consultas extra. */
    @Override
    @EntityGraph(attributePaths = {"ejercicio", "tema", "tema.curso"})
    Page<ResolucionEntity> findAll(Specification<ResolucionEntity> filtro, Pageable pagina);

    /**
     * RN26: borra la resolución en la base; las claves foráneas ON DELETE CASCADE de V1 se llevan sus pasos
     * (con los anidados), parámetros, restricciones, sugerencias y sesiones de práctica.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM resolucion WHERE id = :id", nativeQuery = true)
    int borrar(@Param("id") Integer id);

    /** El ejercicio sin resoluciones también se va (y con él ejercicio_tema). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM ejercicio WHERE id = :id AND NOT EXISTS "
            + "(SELECT 1 FROM resolucion r WHERE r.ejercicio_id = :id)", nativeQuery = true)
    int borrarEjercicioSinResoluciones(@Param("id") Integer id);

    /**
     * "Borrar todo el historial" (Sprint 9, parte E): las resoluciones y, por ON DELETE CASCADE, sus pasos,
     * parámetros, restricciones, sugerencias, sesiones de práctica e intentos. No toca ejemplo ni los catálogos.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM resolucion", nativeQuery = true)
    int borrarTodas();

    /** Los ejercicios (y con ellos ejercicio_tema). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM ejercicio", nativeQuery = true)
    int borrarEjercicios();

    @Query(value = "SELECT COUNT(*) FROM ejercicio", nativeQuery = true)
    long contarEjercicios();
}
