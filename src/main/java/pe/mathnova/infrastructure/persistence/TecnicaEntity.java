package pe.mathnova.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Tabla tecnica de V1 (catálogo de V2 y V3, solo lectura): F01…, L01…, D01…, y los pasos internos de V3. */
@Entity
@Table(name = "tecnica")
@Getter
@NoArgsConstructor
public class TecnicaEntity {

    @Id
    @Column(name = "codigo")
    private String codigo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "categoria", nullable = false)
    private String categoria;

    @Column(name = "campo_min")
    private String campoMin;
}
