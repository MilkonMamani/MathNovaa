package pe.mathnova.infrastructure.persistence;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ejercicio")
@Getter
@Setter
@NoArgsConstructor
public class EjercicioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "expresion_entrada", nullable = false)
    private String expresionEntrada;

    @Column(name = "expresion_normalizada", nullable = false, unique = true)
    private String expresionNormalizada;

    @Column(name = "expresion_latex", nullable = false)
    private String expresionLatex;

    @Column(name = "variables", nullable = false)
    private String variables;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Convert(converter = BooleanoEnteroConverter.class)
    @Column(name = "es_favorito", nullable = false)
    private Boolean esFavorito = Boolean.FALSE;

    @Convert(converter = InstanteConverter.class)
    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @ManyToMany
    @JoinTable(name = "ejercicio_tema", joinColumns = @JoinColumn(name = "ejercicio_id"),
            inverseJoinColumns = @JoinColumn(name = "tema_id"))
    private Set<TemaEntity> temas = new LinkedHashSet<>();
}
