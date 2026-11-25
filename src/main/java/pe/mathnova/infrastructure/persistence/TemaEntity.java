package pe.mathnova.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tema")
@Getter
@NoArgsConstructor
public class TemaEntity {

    @Id
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private CursoEntity curso;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "orden", nullable = false)
    private Integer orden;
}
