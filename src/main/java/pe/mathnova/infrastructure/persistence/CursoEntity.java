package pe.mathnova.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "curso")
@Getter
@NoArgsConstructor
public class CursoEntity {

    @Id
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "orden", nullable = false)
    private Integer orden;
}
