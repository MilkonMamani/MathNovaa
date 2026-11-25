package pe.mathnova.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CursoRepository extends JpaRepository<CursoEntity, Integer> {

    List<CursoEntity> findAllByOrderByOrdenAsc();
}
