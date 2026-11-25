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

    Optional<ResolucionEntity> findByEjercicioAndOperacionCodigoAndClaveParametros(
            EjercicioEntity ejercicio, String operacionCodigo, String claveParametros);

    @Override
    @EntityGraph(attributePaths = {"ejercicio", "tema", "tema.curso"})
    Page<ResolucionEntity> findAll(Specification<ResolucionEntity> filtro, Pageable pagina);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM resolucion WHERE id = :id", nativeQuery = true)
    int borrar(@Param("id") Integer id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM ejercicio WHERE id = :id AND NOT EXISTS "
            + "(SELECT 1 FROM resolucion r WHERE r.ejercicio_id = :id)", nativeQuery = true)
    int borrarEjercicioSinResoluciones(@Param("id") Integer id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM resolucion", nativeQuery = true)
    int borrarTodas();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM ejercicio", nativeQuery = true)
    int borrarEjercicios();

    @Query(value = "SELECT COUNT(*) FROM ejercicio", nativeQuery = true)
    long contarEjercicios();
}
