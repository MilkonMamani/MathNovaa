package pe.mathnova.domain.sugerencias;

import java.util.ArrayList;
import java.util.List;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.factorizacion.Factor;

/**
 * RN15: un valor a que anula el numerador y el denominador da 0/0 en x → a. El valor a es la raíz de un
 * factor que se canceló; si se cancelaron varios, una tarjeta por cada uno, de menor a mayor.
 */
final class LimiteIndeterminado implements ReglaSugerencia {

    @Override
    public String codigo() {
        return "RN15";
    }

    @Override
    public List<Sugerencia> aplica(EjercicioResuelto r) {
        if (!r.esFraccion()) {
            return List.of();
        }
        List<Racional> ceros = new ArrayList<>();
        for (Factor f : r.fraccion().cancelados()) {
            Polinomio p = f.polinomio();
            if (p.gradoEn(r.variable()) == 1 && p.grado() == 1) {
                ceros.add(p.constante().negar().dividir(p.coeficiente(Monomio.de(r.variable(), 1))));
            }
        }
        char x = r.variable();
        List<Sugerencia> tarjetas = new ArrayList<>();
        for (Racional a : ceros.stream().distinct().sorted().toList()) {
            String plantilla = "lim(" + x + "→" + a + ", " + r.texto() + ")";
            tarjetas.add(new Sugerencia(codigo(), "LIMITE", "Límite cuando " + x + " → " + a,
                    x + " = " + a + " anula el numerador y el denominador: en el límite cuando " + x + " → "
                            + a + " hay una indeterminación 0/0.",
                    plantilla, Reglas.latex(plantilla), true));
        }
        return tarjetas;
    }
}
