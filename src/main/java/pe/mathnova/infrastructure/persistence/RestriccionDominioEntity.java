package pe.mathnova.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tabla restriccion_dominio de V1: "x ≠ 2" de una fracción, "x > 0" de un logaritmo (RN14, RN34). */
@Entity
@Table(name = "restriccion_dominio")
@Getter
@Setter
@NoArgsConstructor
public class RestriccionDominioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resolucion_id", nullable = false)
    private ResolucionEntity resolucion;

    @Column(name = "texto", nullable = false)
    private String texto;

    @Column(name = "latex", nullable = false)
    private String latex;

    /** DENOMINADOR, LOGARITMO, RAIZ_PAR, TANGENTE u OTRA. */
    @Column(name = "origen", nullable = false)
    private String origen;
}
