package pe.mathnova.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "operacion")
@Getter
@NoArgsConstructor
public class OperacionEntity {

    @Id
    @Column(name = "codigo")
    private String codigo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "modulo", nullable = false)
    private String modulo;
}
