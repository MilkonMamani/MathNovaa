package pe.mathnova.domain.calculo.derivadas;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.FuncionRacional;
import pe.mathnova.domain.ecuacion.ResolucionEcuacion;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.ImpresoraPizarra;

public record AnalisisDerivada(char variable, Expresion primera, Expresion segunda, List<PuntoCritico> criticos,
                               Tramos crecimiento, Tramos concavidad, List<Inflexion> inflexiones,
                               ResolucionEcuacion ecuacion) {

    public enum Tipo {
        MAXIMO("máximo"), MINIMO("mínimo"), NINGUNO("ni máximo ni mínimo");

        private final String texto;

        Tipo(String texto) {
            this.texto = texto;
        }

        public String texto() {
            return texto;
        }
    }

    public record PuntoCritico(Racional x, Racional y, Tipo tipo, String criterio) {
    }

    public record Inflexion(Racional x, Racional y) {
    }

    public record Tramos(List<Racional> marcas, List<Integer> signos, String aviso) {
        public Tramos {
            marcas = List.copyOf(marcas);
            signos = List.copyOf(signos);
        }
    }

    public AnalisisDerivada {
        criticos = List.copyOf(criticos);
        inflexiones = List.copyOf(inflexiones);
    }

    public static Optional<AnalisisDerivada> de(Expresion f, char x, ResolvedorEcuaciones resolvedor) {
        var letras = Expresiones.variables(f);
        if (letras.size() != 1 || !letras.contains(x)) {
            return Optional.empty();
        }
        Optional<FuncionRacional> fr = FuncionRacional.de(f);
        if (fr.isEmpty()) {
            return Optional.empty();
        }
        try {
            Expresion d1 = FormaFinal.de(Derivador.derivar(f, x).derivada(), x);
            Expresion d2 = FormaFinal.de(Derivador.derivar(d1, x).derivada(), x);
            FuncionRacional f1 = FuncionRacional.de(d1).orElseThrow();
            FuncionRacional f2 = FuncionRacional.de(d2).orElseThrow();
            if (f1.numerador().esCero()) {
                return Optional.empty();
            }
            Ceros noDefinida = ceros(fr.get().denominador(), x, resolvedor);
            ResolucionEcuacion ec = resolvedor.resolver(new Ecuacion(f1.numerador().aExpresion(), Constante.de(0)));
            Ceros c1 = ceros(ec, x);
            Tramos crecimiento = tramos(f1, x, c1, noDefinida);
            List<PuntoCritico> criticos = new ArrayList<>();
            for (Racional c : c1.valores()) {
                Optional<Racional> y = fr.get().evaluar(x, c);
                if (y.isEmpty()) {
                    continue;
                }
                criticos.add(clasificar(c, y.get(), f2, crecimiento, x));
            }
            Ceros c2 = f2.numerador().esConstante() ? new Ceros(List.of(), null)
                    : ceros(resolvedor.resolver(new Ecuacion(f2.numerador().aExpresion(), Constante.de(0))), x);
            Tramos concavidad = tramos(f2, x, c2, noDefinida);
            List<Inflexion> inflexiones = new ArrayList<>();
            if (concavidad.aviso() == null) {
                for (Racional c : c2.valores()) {
                    int i = concavidad.marcas().indexOf(c);
                    Optional<Racional> y = fr.get().evaluar(x, c);
                    if (i >= 0 && y.isPresent() && !concavidad.signos().get(i).equals(concavidad.signos().get(i + 1))) {
                        inflexiones.add(new Inflexion(c, y.get()));
                    }
                }
            }
            return Optional.of(new AnalisisDerivada(x, d1, d2, criticos, crecimiento, concavidad, inflexiones, ec));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private record Ceros(List<Racional> valores, String aviso) {
    }

    private static Ceros ceros(Polinomio p, char x, ResolvedorEcuaciones resolvedor) {
        if (p.esConstante()) {
            return new Ceros(List.of(), null);
        }
        return ceros(resolvedor.resolver(new Ecuacion(p.aExpresion(), Constante.de(0))), x);
    }

    private static Ceros ceros(ResolucionEcuacion r, char x) {
        List<Racional> v = r.soluciones().stream().map(ResolucionEcuacion.Solucion::valor).toList();
        for (Polinomio q : r.sinSolucion()) {
            if (tieneRaizReal(q, x)) {
                return new Ceros(v, "Hay raíces que no son racionales (" + ImpresoraPizarra.imprimir(
                        Simplificador.simplificar(q.aExpresion())) + " = 0): la recta de signos no se arma.");
            }
        }
        return new Ceros(v, null);
    }

    private static boolean tieneRaizReal(Polinomio q, char x) {
        int n = q.gradoEn(x);
        if (n % 2 == 1) {
            return true;
        }
        if (n == 2) {
            Racional a = q.coeficiente(pe.mathnova.domain.algebra.Monomio.de(x, 2));
            Racional b = q.coeficiente(pe.mathnova.domain.algebra.Monomio.de(x, 1));
            Racional c = q.constante();
            return b.multiplicar(b).restar(Racional.de(4).multiplicar(a).multiplicar(c)).signo() >= 0;
        }
        return true;
    }

    private static Tramos tramos(FuncionRacional g, char x, Ceros ceros, Ceros noDefinida) {
        String aviso = ceros.aviso() != null ? ceros.aviso() : noDefinida.aviso();
        if (aviso != null) {
            return new Tramos(List.of(), List.of(), aviso);
        }
        TreeSet<Racional> marcas = new TreeSet<>(ceros.valores());
        marcas.addAll(noDefinida.valores());
        List<Racional> m = new ArrayList<>(marcas);
        List<Integer> signos = new ArrayList<>();
        for (int i = 0; i <= m.size(); i++) {
            Racional prueba;
            if (m.isEmpty()) {
                prueba = Racional.CERO;
            } else if (i == 0) {
                prueba = m.getFirst().restar(Racional.UNO);
            } else if (i == m.size()) {
                prueba = m.getLast().sumar(Racional.UNO);
            } else {
                prueba = m.get(i - 1).sumar(m.get(i)).dividir(Racional.de(2));
            }
            Racional v = g.evaluar(x, prueba).orElse(Racional.CERO);
            signos.add(v.signo() >= 0 ? 1 : -1);
        }
        return new Tramos(m, signos, null);
    }

    private static PuntoCritico clasificar(Racional c, Racional y, FuncionRacional f2, Tramos crecimiento, char x) {
        String en = ImpresoraPizarra.numero(c);
        Optional<Racional> v = f2.evaluar(x, c);
        if (v.isPresent() && !v.get().esCero()) {
            boolean minimo = v.get().signo() > 0;
            return new PuntoCritico(c, y, minimo ? Tipo.MINIMO : Tipo.MAXIMO,
                    "f''(" + en + ") = " + ImpresoraPizarra.numero(v.get()) + (minimo ? " > 0" : " < 0"));
        }
        int i = crecimiento.marcas().indexOf(c);
        if (i < 0 || crecimiento.aviso() != null) {
            return new PuntoCritico(c, y, Tipo.NINGUNO, "f''(" + en + ") = 0 y no se pudo ver el signo de f'");
        }
        int antes = crecimiento.signos().get(i);
        int despues = crecimiento.signos().get(i + 1);
        if (antes == despues) {
            return new PuntoCritico(c, y, Tipo.NINGUNO, "f''(" + en + ") = 0 y f' no cambia de signo");
        }
        return new PuntoCritico(c, y, antes > 0 ? Tipo.MAXIMO : Tipo.MINIMO,
                "f' pasa de " + (antes > 0 ? "+ a −" : "− a +"));
    }
}
