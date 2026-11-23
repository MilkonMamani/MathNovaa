package pe.mathnova.domain.expresion;

/** Recorre el árbol de una Expresion y devuelve un resultado de tipo R. */
public interface Visitante<R> {

    R visitar(Constante c);

    R visitar(Variable v);

    R visitar(ConstanteEspecial c);

    R visitar(Suma s);

    R visitar(Producto p);

    R visitar(Cociente c);

    R visitar(Potencia p);

    R visitar(Negacion n);

    R visitar(Raiz r);

    R visitar(Funcion f);

    R visitar(Limite l);

    R visitar(Derivada d);

    R visitar(DerivadaParcial d);

    R visitar(Integral i);

    R visitar(IntegralMultiple i);

    R visitar(Ecuacion e);

    /** ∇f (Sprint 7). */
    R visitar(Gradiente g);

    /** D_u f(P) (Sprint 7). */
    R visitar(Direccional d);

    /** lim((x, y) → (a, b), f) (Sprint 8). */
    R visitar(LimiteDoble l);

    /** extremos(f) o extremos(f; g = c) (Sprint 8). */
    R visitar(Extremos x);
}
