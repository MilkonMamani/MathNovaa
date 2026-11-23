package pe.mathnova.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EjercicioRepository extends JpaRepository<EjercicioEntity, Integer> {

    Optional<EjercicioEntity> findByExpresionNormalizada(String expresionNormalizada);
}
