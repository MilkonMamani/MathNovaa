package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.Verificacion;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.calculo.limites.ResolucionLimite;
import pe.mathnova.domain.calculo.limites.ResultadoLimite;
import pe.mathnova.domain.ecuacion.ResolucionEcuacion;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.Extension;
import pe.mathnova.domain.factorizacion.Factor;
import pe.mathnova.domain.factorizacion.FormaExtendida;
import pe.mathnova.domain.factorizacion.Irreducibilidad;
import pe.mathnova.domain.factorizacion.Paso;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.ResolucionFunciones;
import pe.mathnova.domain.factorizacion.Simplificacion;
import pe.mathnova.domain.factorizacion.Tecnicas;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.parser.Parser;
import pe.mathnova.presentation.FormateadorTexto;

public record VistaResolucion(String subtitulo, Expresion formula, Etiqueta etiqueta, List<String> notas,
                              List<VistaPaso> pasos, String sinPasos, List<GrupoDescartados> descartados,
                              List<String> verificacion, boolean coincide, List<Alternativa> alternativas,
                              String latexFormula, String ayudaEtiqueta) {

    public VistaResolucion(String subtitulo, Expresion formula, Etiqueta etiqueta, List<String> notas,
                           List<VistaPaso> pasos, String sinPasos, List<GrupoDescartados> descartados,
                           List<String> verificacion, boolean coincide, List<Alternativa> alternativas,
                           String latexFormula) {
        this(subtitulo, formula, etiqueta, notas, pasos, sinPasos, descartados, verificacion, coincide, alternativas,
                latexFormula, null);
    }

    public VistaResolucion(String subtitulo, Expresion formula, Etiqueta etiqueta, List<String> notas,
                           List<VistaPaso> pasos, String sinPasos, List<GrupoDescartados> descartados,
                           List<String> verificacion, boolean coincide, List<Alternativa> alternativas) {
        this(subtitulo, formula, etiqueta, notas, pasos, sinPasos, descartados, verificacion, coincide, alternativas,
                formula == null ? null : ImpresoraLatex.imprimir(formula));
    }

    public enum Etiqueta {
        VERIFICADO("Verificado"),
        NO_COINCIDE("No coincide"),
        IRREDUCIBLE("Irreducible en Q"),
        SIN_FACTORIZACION("Sin factorización en Q"),
        SIN_SOLUCION("Sin solución en Q"),
        IRREDUCIBLE_R("Irreducible en R"),
        APROXIMADO("≈ Raíces aproximadas"),
        NO_FACTORIZABLE("No se factoriza"),
        CALCULADO("Valor exacto"),
        NO_EXISTE("No existe"),
        SIN_CALCULAR("Sin calcular");

        private final String texto;

        Etiqueta(String texto) {
            this.texto = texto;
        }

        public String texto() {
            return texto;
        }
    }

    public record GrupoDescartados(Expresion sobre, List<Descartado> filas) {
    }

    public record Descartado(String nombre, String ayuda, String motivo) {
    }

    public record Alternativa(String codigo, String nombre, String ayuda) {
    }

    public int totalDescartados() {
        return descartados.stream().mapToInt(g -> g.filas().size()).sum();
    }

    public static String nombre(String codigo, boolean codigos) {
        String corto = Tecnicas.nombreCorto(codigo);
        return codigos ? codigo + " · " + corto : corto;
    }

    private static String texto(String t, boolean codigos) {
        return FormateadorTexto.pizarra(codigos ? t : FormateadorTexto.sinCodigos(t));
    }

    public static VistaResolucion de(Resolucion r, Verificacion v, boolean codigos) {
        if (r.extension() != null) {
            return enCampo(r, v, codigos);
        }
        Polinomio entrada = Polinomio.desde(r.entrada());
        List<VistaPaso> pasos = pasos(r.pasos(), null, codigos, 1);
        boolean certificado = !r.irreducibles().isEmpty()
                && r.irreducibles().stream().allMatch(Irreducibilidad::certificada);
        Etiqueta etiqueta;
        if (!r.pasos().isEmpty()) {
            etiqueta = v.coincide() ? Etiqueta.VERIFICADO : Etiqueta.NO_COINCIDE;
        } else {
            etiqueta = certificado ? Etiqueta.IRREDUCIBLE : Etiqueta.SIN_FACTORIZACION;
        }
        int factores = r.resultado().factores().stream().mapToInt(Factor::multiplicidad).sum();
        String estado = r.pasos().isEmpty()
                ? (certificado ? "irreducible en Q" : "sin factorización en Q")
                : factores == 1 ? "1 factor en Q" : factores + " factores en Q";
        String subtitulo = "Polinomio de grado " + (entrada.esCero() ? 0 : entrada.grado()) + " · " + estado;

        return new VistaResolucion(subtitulo, r.resultado().aExpresion(), etiqueta, notaDeFactores(r, codigos), pasos,
                r.pasos().isEmpty() ? sinPasos(r, certificado, codigos) : null, descartados(r.descartados(), codigos),
                verificacion(v), v.coincide(),
                r.alternativas().stream()
                        .map(c -> new Alternativa(c, nombre(c, codigos), Tecnicas.nombre(c))).toList());
    }

    static VistaResolucion enCampo(Resolucion r, Verificacion v, boolean codigos) {
        Extension ext = r.extension();
        FormaExtendida forma = ext.resultado();
        Polinomio entrada = Polinomio.desde(r.entrada());
        List<VistaPaso> pasos = new ArrayList<>(pasos(r.pasos(), null, codigos, 1));
        for (Extension.Paso p : ext.pasos()) {
            pasos.add(new VistaPaso(pasos.size() + 1, p.codigoTecnica(), nombre(p.codigoTecnica(), codigos),
                    p.nombreTecnica(), texto(p.justificacion(), codigos), p.antes(), p.trabajo(), p.despues()));
        }
        String c = ext.campo().name();
        Etiqueta etiqueta;
        String ayuda = null;
        if (ext.aproximada()) {
            etiqueta = Etiqueta.APROXIMADO;
            ayuda = "Durand-Kerner y Newton con 50 cifras; se muestran 10 decimales (Abel–Ruffini, RN30)";
        } else if (pasos.isEmpty()) {
            boolean lineales = forma.factores().stream().allMatch(f -> f.polinomio().grado() <= 1);
            etiqueta = lineales ? Etiqueta.VERIFICADO
                    : ext.campo() == Campo.R ? Etiqueta.IRREDUCIBLE_R : Etiqueta.SIN_FACTORIZACION;
        } else {
            etiqueta = v.coincide() ? Etiqueta.VERIFICADO : Etiqueta.NO_COINCIDE;
            if (v.numerica() && v.coincide()) {
                ayuda = Verificacion.AYUDA_NUMERICA;
            }
        }
        int factores = forma.factores().stream().mapToInt(FormaExtendida.Factor::multiplicidad).sum();
        String estado = ext.aproximada() ? "raíces aproximadas en " + c
                : factores == 1 ? "1 factor en " + c : factores + " factores en " + c;
        String subtitulo = "Polinomio de grado " + (entrada.esCero() ? 0 : entrada.grado()) + " · " + estado;

        List<String> notas = new ArrayList<>();
        ext.notas().forEach(n -> notas.add(FormateadorTexto.pizarra(n)));
        if (ext.aproximada()) {
            notas.add((ext.campo() == Campo.R ? "Raíces reales ≈ " : "Raíces ≈ ")
                    + FormateadorTexto.pizarraConNegativos(String.join(", ", ext.aproximadas())));
        }
        if (notas.isEmpty() && factores > 0 && forma.factores().stream().allMatch(f -> f.polinomio().grado() == 1)) {
            notas.add(factores == 1 ? "El factor es de grado 1: ya no se puede factorizar más."
                    : "Los " + factores + " factores son de grado 1: ya no se pueden factorizar más.");
        }
        String sinPasos = pasos.isEmpty() ? "No se aplicó ningún método: el polinomio no se parte en " + c + "." : null;
        String latex = FactorizacionService.latex(r);
        var legible = FormaLegible.de(r);
        if (legible.isPresent()) {
            latex = legible.get().latex();
            notas.clear();
            notas.addAll(legible.get().notas());
            for (int i = 0; i < pasos.size(); i++) {
                if ("F27".equals(pasos.get(i).codigo())) {
                    pasos.set(i, pasos.get(i).conDesarrollo(legible.get().desarrollo()));
                }
            }
        }
        return new VistaResolucion(subtitulo, forma.aExpresion(), etiqueta,
                notas.isEmpty() ? List.of() : List.of(String.join(" ", notas)), pasos, sinPasos,
                descartados(r.descartados(), codigos), verificacion(v), v.coincide(),
                r.alternativas().stream().map(a -> new Alternativa(a, nombre(a, codigos), Tecnicas.nombre(a))).toList(),
                latex, ayuda);
    }

    public static VistaResolucion de(ResolucionFunciones r, Verificacion v, boolean codigos) {
        List<String> notas = new ArrayList<>();
        if (!r.restricciones().isEmpty()) {
            notas.add("Restricciones del dominio: " + FormateadorTexto.pizarra(String.join(", ", r.restricciones())));
        }
        if (!r.factorizable()) {
            return new VistaResolucion("Expresión con funciones · no se factoriza", r.entrada(),
                    Etiqueta.NO_FACTORIZABLE, notas, List.of(), r.mensaje() + ".", List.of(), List.of(), false,
                    List.of());
        }
        List<VistaPaso> pasos = new ArrayList<>();
        TrabajoIntermedio.Sustitucion sus = r.sustitucion();
        if (!r.identidades().isEmpty()) {
            pasos.add(new VistaPaso(1, "T04", nombre("T04", codigos), Tecnicas.nombre("T04"),
                    "Las identidades dejan la expresión como un polinomio en sus funciones.", r.entrada(),
                    new TrabajoIntermedio.Agrupacion(r.identidades().stream().map(FormateadorTexto::pizarra).toList()),
                    null));
        }
        String codigo = r.codigo().equals("T04") ? "T03" : r.codigo();
        pasos.add(new VistaPaso(pasos.size() + 1, codigo, nombre(codigo, codigos), Tecnicas.nombre(codigo),
                FormateadorTexto.pizarra("Es polinómica en " + sus.definicion() + ": se sustituye, se factoriza en "
                        + sus.variableNueva() + " y se deshace la sustitución."),
                r.entrada(), sus, r.resultado()));
        pasos.addAll(pasos(r.enLetras().pasos(), "en " + sus.variableNueva(), codigos, pasos.size() + 1));
        Etiqueta etiqueta = v.coincide() ? Etiqueta.VERIFICADO : Etiqueta.NO_COINCIDE;
        return new VistaResolucion("Expresión con funciones · " + FormateadorTexto.pizarra(sus.definicion()),
                r.resultado(), etiqueta, notas, pasos, null, descartados(r.enLetras().descartados(), codigos),
                verificacion(v), v.coincide(), List.of(), ImpresoraLatex.imprimir(r.resultado()),
                v.coincide() ? Verificacion.AYUDA_NUMERICA : null);
    }

    public static VistaResolucion de(Simplificacion s, Verificacion v, boolean codigos) {
        List<VistaPaso> pasos = new ArrayList<>(pasos(s.numerador().pasos(), "numerador", codigos, 1));
        pasos.addAll(pasos(s.denominador().pasos(), "denominador", codigos, pasos.size() + 1));
        String original = "(" + s.numerador().resultado().expandir() + ")/(" + s.denominador().resultado().expandir() + ")";
        var trabajo = new TrabajoIntermedio.Agrupacion(List.of(
                FormateadorTexto.pizarra(original) + " = " + factorizada(s),
                "= " + FormateadorTexto.pizarra(s.fraccion())));
        pasos.add(new VistaPaso(pasos.size() + 1, "P01", nombre("P01", codigos), Tecnicas.nombre("P01"),
                s.justificacion(), s.entrada(), trabajo, s.aExpresion()));
        if (s.division() != null) {
            pasos.add(new VistaPaso(pasos.size() + 1, "P02", nombre("P02", codigos), Tecnicas.nombre("P02"),
                    "El grado del numerador no es menor que el del denominador: al dividir queda "
                            + FormateadorTexto.pizarra(s.division()),
                    s.aExpresion(), null, Parser.parse(s.division())));
        }
        List<String> notas = new ArrayList<>();
        if (!s.restricciones().isEmpty()) {
            notas.add("Restricciones del denominador original: "
                    + FormateadorTexto.pizarra(String.join(", ", s.restricciones()).replace("≠ -", "≠ −")));
        }
        List<GrupoDescartados> descartados = new ArrayList<>(descartados(s.numerador().descartados(), codigos));
        descartados.addAll(descartados(s.denominador().descartados(), codigos));
        String subtitulo = "Fracción algebraica · "
                + (s.cancelados().isEmpty() ? "ya estaba simplificada" : "simplificada en Q");
        return new VistaResolucion(subtitulo, s.aExpresion(), v.coincide() ? Etiqueta.VERIFICADO : Etiqueta.NO_COINCIDE,
                notas, pasos, null, descartados, verificacion(v), v.coincide(), List.of());
    }

    public static VistaResolucion de(ResolucionEcuacion r, boolean codigos) {
        List<VistaPaso> filas = new ArrayList<>();
        Ecuacion original = r.entrada();
        Expresion igualadaACero = new Ecuacion(r.polinomio().aExpresion(), Constante.de(0));
        Resolucion f = r.factorizacion();
        for (ResolucionEcuacion.PasoEcuacion p : r.pasos()) {
            int orden = filas.size() + 1;
            List<String> lineas = p.lineas().stream().map(FormateadorTexto::pizarraConNegativos).toList();
            TrabajoIntermedio trabajo = new TrabajoIntermedio.Agrupacion(lineas);
            switch (p.tipo()) {
                case PASAR_A_UN_LADO -> filas.add(new VistaPaso(orden, "E01", p.titulo(), p.titulo(),
                        p.detalle(), original, trabajo, igualadaACero));
                case FACTORIZAR -> {
                    List<VistaPaso> deLaFactorizacion = f == null ? List.of() : pasos(f.pasos(), null, codigos, orden);
                    if (deLaFactorizacion.isEmpty()) {
                        filas.add(new VistaPaso(orden, "E02", p.titulo(), p.titulo(),
                                p.detalle() + " No se aplicó ningún método: ya no se factoriza en Q.",
                                igualadaACero, null, factorizadaIgualACero(r)));
                    } else {
                        filas.addAll(deLaFactorizacion);
                    }
                }
                case IGUALAR_A_CERO -> filas.add(new VistaPaso(orden, "E03", p.titulo(), p.titulo(), p.detalle(),
                        factorizadaIgualACero(r), trabajo, null));
                case DESPEJAR -> filas.add(new VistaPaso(orden, "E04", p.titulo(), p.titulo(), p.detalle(),
                        null, trabajo, null));
            }
        }

        Polinomio izquierda = Polinomio.desde(original.izquierda());
        Polinomio derecha = Polinomio.desde(original.derecha());
        List<String> verificacion = new ArrayList<>();
        boolean coincide = true;
        for (ResolucionEcuacion.Solucion s : r.soluciones()) {
            Map<Character, Racional> valor = Map.of(r.variable(), s.valor());
            Racional a = izquierda.evaluar(valor);
            Racional b = derecha.evaluar(valor);
            boolean ok = a.equals(b);
            coincide &= ok;
            verificacion.add(FormateadorTexto.pizarraConNegativos(r.variable() + " = " + s.valor() + ":  " + a + " = " + b)
                    + (ok ? "  ✓" : "  ✗"));
        }

        List<String> notas = new ArrayList<>();
        for (Polinomio p : r.sinSolucion()) {
            notas.add(FormateadorTexto.pizarra(p + " = 0") + " no tiene solución en Q.");
        }
        int grado = r.polinomio().esCero() ? 0 : r.polinomio().grado();
        String estado = switch (r.tipo()) {
            case IDENTIDAD -> "se cumple para todo " + r.variable();
            case SIN_SOLUCION -> "sin solución en Q";
            case CON_SOLUCIONES -> r.soluciones().size() == 1 ? "1 solución en Q" : r.soluciones().size() + " soluciones en Q";
        };
        Etiqueta etiqueta = r.tipo() == ResolucionEcuacion.Tipo.CON_SOLUCIONES
                ? (coincide ? Etiqueta.VERIFICADO : Etiqueta.NO_COINCIDE)
                : Etiqueta.SIN_SOLUCION;
        List<Alternativa> alternativas = f == null ? List.of() : f.alternativas().stream()
                .map(c -> new Alternativa(c, nombre(c, codigos), Tecnicas.nombre(c))).toList();
        return new VistaResolucion("Ecuación de grado " + grado + " · " + estado, null, etiqueta, notas, filas, null,
                f == null ? List.of() : descartados(f.descartados(), codigos), verificacion,
                coincide && r.tipo() == ResolucionEcuacion.Tipo.CON_SOLUCIONES, alternativas, latexSoluciones(r));
    }

    public static VistaResolucion de(ResolucionLimite r, boolean codigos) {
        List<VistaPaso> pasos = new ArrayList<>();
        for (PasoLimite p : r.pasos()) {
            pasos.add(pasoLimite(p, pasos.size() + 1, codigos));
        }
        ResultadoLimite res = r.resultado();
        String limite = ImpresoraLatex.imprimir(r.entrada());
        String latex = res instanceof ResultadoLimite.NoCalculado ? limite : limite + " = " + res.latex();
        Etiqueta etiqueta = switch (res) {
            case ResultadoLimite.Finito f -> Etiqueta.CALCULADO;
            case ResultadoLimite.Infinito i -> Etiqueta.CALCULADO;
            case ResultadoLimite.NoExiste n -> Etiqueta.NO_EXISTE;
            case ResultadoLimite.NoCalculado n -> Etiqueta.SIN_CALCULAR;
        };
        List<String> notas = new ArrayList<>();
        notas.add(r.indeterminacion() == null ? "La evaluación directa decide: no hay indeterminación."
                : "Indeterminación encontrada: " + r.indeterminacion().texto() + ".");
        if (r.metodoFinal() != null) {
            notas.add("Método final: " + Tecnicas.nombre(r.metodoFinal()) + ".");
        }
        if (res instanceof ResultadoLimite.NoExiste n) {
            notas.add("No existe: " + FormateadorTexto.pizarra(n.motivo()) + ".");
        } else if (res instanceof ResultadoLimite.NoCalculado n) {
            notas.add(n.motivo() + ".");
        }
        StringBuilder subtitulo = new StringBuilder("Límite cuando " + r.variable() + " → " + punto(r.entrada()));
        if (r.indeterminacion() != null) {
            subtitulo.append(" · ").append(r.indeterminacion().texto());
        }
        if (r.metodoFinal() != null) {
            subtitulo.append(" · ").append(Tecnicas.nombre(r.metodoFinal()));
        }
        String ans = res instanceof ResultadoLimite.Finito ? res.latex() : null;
        return new VistaResolucion(subtitulo.toString(), null, etiqueta, List.of(String.join(" ", notas)), pasos, null,
                List.of(), List.of(), false, List.of(), latex, ans == null ? null : "Valor exacto: " + res.texto());
    }

    static String punto(Limite l) {
        return switch (l.punto()) {
            case Limite.Infinito i -> i.positivo() ? "∞" : "−∞";
            case Limite.Finito f -> FormateadorTexto.pizarraConNegativos(ImpresoraTexto.imprimir(f.valor()))
                    + (l.lado() == Limite.Lado.DERECHA ? "⁺" : l.lado() == Limite.Lado.IZQUIERDA ? "⁻" : "");
        };
    }

    public static VistaResolucion de(pe.mathnova.domain.calculo.derivadas.ResolucionDerivada r, boolean codigos) {
        List<VistaPaso> pasos = new ArrayList<>();
        for (PasoLimite p : r.pasos()) {
            pasos.add(pasoLimite(p, pasos.size() + 1, codigos));
        }
        java.util.LinkedHashSet<String> reglas = new java.util.LinkedHashSet<>();
        for (String c : r.codigos()) {
            if (c.matches("D0\\d")) {
                reglas.add(pe.mathnova.domain.calculo.derivadas.PasoDerivada.nombre(c));
            }
        }
        String subtitulo = r.esImplicita() ? "Derivación implícita respecto de " + r.variable()
                : (r.orden() == 1 ? "Derivada" : "Derivada de orden " + r.orden()) + " respecto de " + r.variable();
        if (!r.pasos().isEmpty() && !r.esImplicita() && r.orden() == 1) {
            subtitulo += " · " + r.pasos().getFirst().titulo();
        }
        List<String> notas = new ArrayList<>();
        if (!reglas.isEmpty()) {
            notas.add("Reglas usadas: " + String.join(", ", reglas) + ".");
        }
        if (r.analisis() != null && !r.analisis().criticos().isEmpty()) {
            List<String> pc = new ArrayList<>();
            for (var c : r.analisis().criticos()) {
                pc.add(r.variable() + " = " + FormateadorTexto.numero(c.x()) + " (" + c.tipo().texto() + ")");
            }
            notas.add("Puntos críticos: " + String.join(", ", pc) + ".");
        }
        Etiqueta etiqueta = r.verificada() ? Etiqueta.VERIFICADO : Etiqueta.NO_COINCIDE;
        String ayuda = r.esImplicita() ? "Verificado: el valor de " + r.implicita() + "' anula la derivada de los dos lados"
                : "Verificado con el cociente incremental en tres puntos (h = 10⁻²⁰)";
        return new VistaResolucion(subtitulo, null, etiqueta, notas.isEmpty() ? List.of() : List.of(String.join(" ", notas)),
                pasos, null, List.of(), List.of(), false, List.of(), r.latex(), r.verificada() ? ayuda : null);
    }

    public static VistaResolucion de(pe.mathnova.domain.calculo.integrales.ResolucionIntegral r, boolean codigos) {
        List<VistaPaso> pasos = new ArrayList<>();
        for (PasoLimite p : r.pasos()) {
            pasos.add(pasoLimite(p, pasos.size() + 1, codigos));
        }
        String tipo = r.definida() ? "Integral definida" : "Integral indefinida";
        String subtitulo = tipo + " respecto de " + r.variable() + (r.metodo() == null ? ""
                : " · " + pe.mathnova.domain.calculo.integrales.MotorIntegrales.nombre(r.metodo()));
        Etiqueta etiqueta = !r.resuelta() ? Etiqueta.SIN_CALCULAR : r.verificada() ? Etiqueta.VERIFICADO
                : Etiqueta.NO_COINCIDE;
        List<String> notas = new ArrayList<>();
        if (!r.resuelta()) {
            notas.add(r.motivo());
        } else if (!r.definida()) {
            notas.add("La constante C es cualquier número: todas las antiderivadas difieren en una constante.");
        }
        List<String> verificacion = new ArrayList<>();
        if (r.verificacion() != null) {
            String integrando = ImpresoraPizarraTexto.de(r.integral().cuerpo());
            String f = ImpresoraPizarraTexto.de(r.antiderivada());
            verificacion.add("d/d" + r.variable() + "(" + f + ") = " + ImpresoraPizarraTexto.de(r.verificacion().derivada()));
            verificacion.add("Integrando: " + integrando + (r.coeficiente().equals(Racional.UNO) ? ""
                    : " (por " + FormateadorTexto.numero(r.coeficiente()) + ")"));
            verificacion.add(r.verificacion().exacta() ? "Comparación exacta" : "Comparación numérica en cinco puntos");
        }
        boolean coincide = r.verificada();
        String ayuda = !coincide ? null : r.verificacion().exacta()
                ? "Verificado: la derivada del resultado es el integrando (exacto)"
                : "Verificado: la derivada del resultado coincide con el integrando en cinco puntos";
        String sinPasos = r.pasos().isEmpty() ? r.motivo() : null;
        return new VistaResolucion(subtitulo, null, etiqueta, notas, pasos, sinPasos, List.of(),
                verificacion, coincide, List.of(), r.latex(), ayuda);
    }

    public static VistaResolucion de(pe.mathnova.domain.calculo.parciales.ResolucionParcial r, boolean codigos) {
        List<VistaPaso> pasos = new ArrayList<>();
        for (PasoLimite p : r.pasos()) {
            pasos.add(pasoLimite(p, pasos.size() + 1, codigos));
        }
        String letras = String.join(", ", r.variables().stream().map(String::valueOf).toList());
        String subtitulo = switch (r.tipo()) {
            case PARCIAL -> "Derivada parcial de f(" + letras + ")";
            case GRADIENTE -> "Gradiente de f(" + letras + ")";
            case DIRECCIONAL -> "Derivada direccional de f(" + letras + ")";
            case CADENA -> "Regla de la cadena";
        };
        List<String> notas = new ArrayList<>();
        notas.add("Dominio: " + pe.mathnova.domain.calculo.parciales.MotorParciales.textoDominio(r.dominio(),
                r.variables()) + ".");
        Etiqueta etiqueta = r.verificada() ? Etiqueta.VERIFICADO : Etiqueta.NO_COINCIDE;
        String ayuda = r.verificada() ? "Verificado con el cociente incremental (h = 10⁻²²) en un punto" : null;
        return new VistaResolucion(subtitulo, null, etiqueta, notas, pasos, null, List.of(), List.of(), false,
                List.of(), r.latex(), ayuda);
    }

    private static List<VistaPaso> pasosDe(List<PasoLimite> ps, boolean codigos) {
        List<VistaPaso> pasos = new ArrayList<>();
        for (PasoLimite p : ps) {
            pasos.add(pasoLimite(p, pasos.size() + 1, codigos));
        }
        return pasos;
    }

    public static VistaResolucion de(pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble r, boolean codigos) {
        String subtitulo = "Límite doble" + (r.metodo() == null ? "" : " · " + Tecnicas.nombre(r.metodo()));
        Etiqueta etiqueta = switch (r.estado()) {
            case EXISTE -> Etiqueta.CALCULADO;
            case NO_EXISTE -> Etiqueta.NO_EXISTE;
            case NO_DECIDIDO -> Etiqueta.SIN_CALCULAR;
        };
        List<String> notas = r.motivo() == null ? List.of() : List.of(r.motivo() + ".");
        return new VistaResolucion(subtitulo, null, etiqueta, notas, pasosDe(r.pasos(), codigos), null, List.of(),
                List.of(), false, List.of(), r.latex(), r.existe() ? "Valor exacto: " + r.texto() : null);
    }

    public static VistaResolucion de(pe.mathnova.domain.calculo.extremos.ResolucionExtremos r, boolean codigos) {
        String subtitulo = r.tipo() == pe.mathnova.domain.calculo.extremos.ResolucionExtremos.Tipo.HESSIANO
                ? "Puntos críticos · Criterio del Hessiano" : "Extremos con restricción · Multiplicadores de Lagrange";
        List<String> notas = new ArrayList<>();
        notas.add(r.texto() + ".");
        notas.addAll(r.avisos());
        Etiqueta etiqueta = r.puntos().isEmpty() ? Etiqueta.SIN_CALCULAR : Etiqueta.CALCULADO;
        return new VistaResolucion(subtitulo, null, etiqueta, notas, pasosDe(r.pasos(), codigos), null, List.of(),
                List.of(), false, List.of(), r.latex(), r.puntos().isEmpty() ? null
                : "Puntos con coordenadas exactas, por sustitución");
    }

    public static VistaResolucion de(pe.mathnova.domain.calculo.multiples.ResolucionIntegralMultiple r,
                                     boolean codigos) {
        String tipo = r.entrada().esDoble() ? "Integral doble" : "Integral triple";
        String subtitulo = tipo + (r.coordenadas() == pe.mathnova.domain.expresion.IntegralMultiple.Coordenadas.CARTESIANAS
                ? r.invertida() ? " · Cambio de orden" : " · Integral iterada"
                : " · Coordenadas " + r.coordenadas().nombre());
        Etiqueta etiqueta = Etiqueta.SIN_CALCULAR;
        String ayuda = null;
        if (r.resuelta()) {
            etiqueta = Etiqueta.CALCULADO;
            var s = pe.mathnova.domain.calculo.multiples.MotorIntegralesMultiples.simpson(r,
                    r.efectiva().esDoble() ? ContenidoIntegralMultiple.SIMPSON_DOBLE
                            : ContenidoIntegralMultiple.SIMPSON_TRIPLE);
            var d = pe.mathnova.domain.calculo.multiples.MotorIntegralesMultiples.decimal(r);
            if (s.isPresent() && d.isPresent()) {
                boolean coincide = s.get().subtract(d.get()).abs().compareTo(new java.math.BigDecimal("1e-4")
                        .multiply(d.get().abs().max(java.math.BigDecimal.ONE))) < 0;
                etiqueta = coincide ? Etiqueta.VERIFICADO : Etiqueta.NO_COINCIDE;
                ayuda = coincide ? "Verificado con la regla de Simpson en " + (r.efectiva().esDoble() ? "2D" : "3D")
                        : null;
            }
        }
        List<String> notas = r.resuelta() ? List.of() : List.of(r.motivo() + ".");
        return new VistaResolucion(subtitulo, null, etiqueta, notas, pasosDe(r.pasos(), codigos),
                r.pasos().isEmpty() ? r.motivo() : null, List.of(), List.of(), false, List.of(), r.latex(), ayuda);
    }

    private static final class ImpresoraPizarraTexto {
        static String de(Expresion e) {
            return pe.mathnova.domain.expresion.ImpresoraPizarra.imprimir(e);
        }
    }

    private static VistaPaso pasoLimite(PasoLimite p, int orden, boolean codigos) {
        List<VistaPaso> hijos = new ArrayList<>();
        for (PasoLimite h : p.hijos()) {
            hijos.add(pasoLimite(h, hijos.size() + 1, codigos));
        }
        TrabajoIntermedio trabajo = p.trabajo() instanceof TrabajoIntermedio.Agrupacion a
                ? new TrabajoIntermedio.Agrupacion(a.grupos().stream().map(FormateadorTexto::pizarraConNegativos).toList())
                : p.trabajo();
        String titulo = codigos ? p.codigo() + " · " + p.titulo() : p.titulo();
        return new VistaPaso(orden, p.codigo(), titulo, Tecnicas.nombre(p.codigo()), texto(p.justificacion(), codigos),
                p.antes(), trabajo, p.despues(), p.grupo(), hijos);
    }

    private static Expresion factorizadaIgualACero(ResolucionEcuacion r) {
        Expresion izquierda = r.factorizacion() == null ? r.polinomio().aExpresion()
                : r.factorizacion().resultado().aExpresion();
        return new Ecuacion(izquierda, Constante.de(0));
    }

    static String latexSoluciones(ResolucionEcuacion r) {
        return switch (r.tipo()) {
            case IDENTIDAD -> "\\text{Se cumple para todo } " + r.variable();
            case SIN_SOLUCION -> "\\text{Sin solución en } \\mathbb{Q}";
            case CON_SOLUCIONES -> {
                List<String> partes = new ArrayList<>();
                List<ResolucionEcuacion.Solucion> s = r.soluciones();
                for (int i = 0; i < s.size(); i++) {
                    String nombre = s.size() == 1 ? String.valueOf(r.variable()) : r.variable() + "_{" + (i + 1) + "}";
                    String parte = nombre + " = " + ImpresoraLatex.imprimir(new Constante(s.get(i).valor()));
                    if (s.get(i).multiplicidad() > 1) {
                        parte += "\\ \\text{(" + s.get(i).nombreMultiplicidad() + ")}";
                    }
                    partes.add(parte);
                }
                yield String.join(",\\quad ", partes);
            }
        };
    }

    static List<VistaPaso> pasos(List<Paso> pasos, String grupo, boolean codigos, int desde) {
        List<VistaPaso> filas = new ArrayList<>();
        String sufijo = grupo == null ? "" : " · " + grupo;
        int i = 0;
        while (i < pasos.size()) {
            Paso p = pasos.get(i);
            int largo = PasosDeRuffini.largoDeLaCadena(pasos, i);
            int orden = desde + filas.size();
            if (largo > 1) {
                List<Paso> cadena = pasos.subList(i, i + largo);
                TrabajoIntermedio.TablaRuffini tabla = PasosDeRuffini.tablaUnica(cadena);
                String titulo = nombre("F16", codigos) + " (" + tabla.divisiones().size() + " raíces)" + sufijo;
                filas.add(new VistaPaso(orden, "F16", titulo, Tecnicas.nombre("F16"),
                        PasosDeRuffini.justificacion(cadena, tabla), p.antes().aExpresion(), tabla,
                        PasosDeRuffini.despues(cadena)));
            } else {
                filas.add(new VistaPaso(orden, p.codigoTecnica(), nombre(p.codigoTecnica(), codigos) + sufijo,
                        p.nombreTecnica(), texto(p.justificacion(), codigos), p.antes().aExpresion(), p.trabajo(),
                        p.despuesComoForma().aExpresion()));
            }
            i += largo;
        }
        return filas;
    }

    static List<String> notaDeFactores(Resolucion r, boolean codigos) {
        List<Factor> factores = r.resultado().factores();
        if (factores.isEmpty()) {
            return List.of();
        }
        List<Irreducibilidad> noLineales = r.irreducibles().stream().filter(i -> i.factor().grado() > 1).toList();
        if (noLineales.isEmpty()) {
            int n = factores.stream().mapToInt(Factor::multiplicidad).sum();
            return List.of(n == 1 ? "El factor es de grado 1: ya no se puede factorizar más."
                    : "Los " + n + " factores son de grado 1: ya no se pueden factorizar más.");
        }
        List<String> frases = new ArrayList<>();
        for (Irreducibilidad i : noLineales) {
            String f = FormateadorTexto.pizarra(i.factor().toString());
            String j = texto(i.justificacion(), codigos);
            if (!i.certificada()) {
                frases.add(f + ": " + minuscula(j) + punto(j));
            } else if (j.startsWith("No se puede factorizar en Q")) {
                frases.add(f + " " + minuscula(j) + punto(j));
            } else {
                frases.add(f + " no se factoriza en Q porque " + (j.startsWith("Δ") ? j : minuscula(j)) + punto(j));
            }
        }
        return List.of(String.join(" ", frases));
    }

    private static String minuscula(String t) {
        return t.isEmpty() ? t : Character.toLowerCase(t.charAt(0)) + t.substring(1);
    }

    private static String punto(String t) {
        return t.endsWith(".") ? "" : ".";
    }

    private static String factorizada(Simplificacion s) {
        String num = s.numerador().resultado().aTexto();
        String den = s.denominador().resultado().aTexto();
        return FormateadorTexto.pizarra(envolver(num) + "/" + envolver(den));
    }

    private static String envolver(String t) {
        return t.startsWith("(") && t.endsWith(")") && t.indexOf(')') == t.length() - 1 ? t : "(" + t + ")";
    }

    private static String sinPasos(Resolucion r, boolean certificado, boolean codigos) {
        if (r.irreducibles().isEmpty()) {
            return "No se aplicó ningún método: la expresión ya está factorizada.";
        }
        StringBuilder sb = new StringBuilder(certificado
                ? "No se aplicó ningún método: el polinomio no se puede factorizar en Q."
                : "No se aplicó ningún método: no se encontró una factorización en Q.");
        for (Irreducibilidad i : r.irreducibles()) {
            sb.append("\n").append(texto(i.justificacion(), codigos));
        }
        return sb.toString();
    }

    static List<GrupoDescartados> descartados(List<Paso> descartados, boolean codigos) {
        Map<Polinomio, List<Descartado>> grupos = new LinkedHashMap<>();
        for (Paso d : descartados) {
            grupos.computeIfAbsent(d.antes(), k -> new ArrayList<>()).add(new Descartado(
                    nombre(d.codigoTecnica(), codigos), d.nombreTecnica(), texto(d.motivoDescarte(), codigos)));
        }
        List<GrupoDescartados> r = new ArrayList<>();
        grupos.forEach((p, filas) -> r.add(new GrupoDescartados(p.aExpresion(), List.copyOf(filas))));
        return r;
    }

    private static List<String> verificacion(Verificacion v) {
        List<String> lineas = new ArrayList<>();
        v.productos().forEach(p -> lineas.add(FormateadorTexto.pizarra(p)));
        lineas.add(FormateadorTexto.pizarra(v.resultado() + " = " + v.expandido()));
        return lineas;
    }
}
