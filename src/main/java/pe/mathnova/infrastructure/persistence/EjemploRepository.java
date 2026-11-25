package pe.mathnova.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EjemploRepository extends JpaRepository<EjemploEntity, Integer> {

    @Query("select e from EjemploEntity e where e.tema.id = :temaId order by e.orden, e.id")
    List<EjemploEntity> deTema(@Param("temaId") Integer temaId);

    @Query("select e from EjemploEntity e join fetch e.tema t join fetch t.curso c order by c.orden, t.orden, e.orden, e.id")
    List<EjemploEntity> todos();

    @Query("select e.tema.id, count(e) from EjemploEntity e group by e.tema.id")
    List<Object[]> cantidadPorTema();
}
