package pe.mathnova.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IntentoPasoRepository extends JpaRepository<IntentoPasoEntity, Integer> {

    List<IntentoPasoEntity> findBySesionIdOrderByIdAsc(Integer sesionId);
}
