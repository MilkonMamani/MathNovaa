package pe.mathnova.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TemaRepository extends JpaRepository<TemaEntity, Integer> {

    Optional<TemaEntity> findByNombre(String nombre);
}
