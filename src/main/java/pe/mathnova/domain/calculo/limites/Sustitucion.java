package pe.mathnova.domain.calculo.limites;

import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.Variable;

public final class Sustitucion {

    private Sustitucion() {
    }

    public static Expresion sustituir(Expresion e, char variable, Expresion valor) {
        return switch (e) {
            case Variable v -> v.nombre() == variable ? valor : v;
            case Suma s -> new Suma(s.terminos().stream().map(t -> sustituir(t, variable, valor)).toList());
            case Producto p -> new Producto(p.factores().stream().map(f -> sustituir(f, variable, valor)).toList());
            case Cociente c -> new Cociente(sustituir(c.numerador(), variable, valor),
                    sustituir(c.denominador(), variable, valor));
            case Potencia p -> new Potencia(sustituir(p.base(), variable, valor), sustituir(p.exponente(), variable, valor));
            case Negacion n -> new Negacion(sustituir(n.argumento(), variable, valor));
            case Raiz r -> new Raiz(r.indice(), sustituir(r.radicando(), variable, valor));
            case Funcion f -> new Funcion(f.tipo(), sustituir(f.argumento(), variable, valor), f.base());
            default -> e;
        };
    }
}
