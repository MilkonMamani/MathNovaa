package pe.mathnova.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResolucionRepository extends JpaRepository<ResolucionEntity, Integer> {

    /** RN25: la resolución de un ejercicio con una operación y una clave de parámetros. */
    Optional<ResolucionEntity> findByEjercicioAndOperacionCodigoAndClaveParametros(
            EjercicioEntity ejercicio, String operacionCodigo, String claveParametros);
}
