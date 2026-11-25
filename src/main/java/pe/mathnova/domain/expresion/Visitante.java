package pe.mathnova.domain.expresion;

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

    R visitar(Gradiente g);

    R visitar(Direccional d);

    R visitar(LimiteDoble l);

    R visitar(Extremos x);
}
