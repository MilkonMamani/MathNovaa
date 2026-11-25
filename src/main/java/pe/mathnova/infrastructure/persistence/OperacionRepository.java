package pe.mathnova.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacionRepository extends JpaRepository<OperacionEntity, String> {
}
