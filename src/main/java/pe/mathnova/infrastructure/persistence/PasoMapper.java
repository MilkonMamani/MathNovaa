package pe.mathnova.infrastructure.persistence;

import java.util.List;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.factorizacion.FormaFactorizada;
import pe.mathnova.domain.factorizacion.Paso;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;

/** Paso del dominio → fila de la tabla paso, con el trabajo intermedio como JSON. */
public final class PasoMapper {

    private PasoMapper() {
    }

    public static PasoEntity aEntidad(Paso p, int orden) {
        PasoEntity e = new PasoEntity();
        e.setOrden(orden);
        e.setTecnicaCodigo(p.codigoTecnica());
        e.setJustificacion(p.justificacion());
        e.setAntesTexto(p.antes().toString());
        e.setAntesLatex(ImpresoraLatex.imprimir(p.antes().aExpresion()));
        FormaFactorizada despues = p.despuesComoForma();
        if (despues != null) {
            e.setDespuesTexto(despues.aTexto());
            e.setDespuesLatex(ImpresoraLatex.imprimir(despues.aExpresion()));
        }
        if (p.trabajo() != null) {
            e.setTrabajoTipo(tipo(p.trabajo()));
            e.setTrabajoJson(json(p.trabajo()));
        }
        e.setEsDescartado(p.descartado());
        e.setMotivoDescarte(p.motivoDescarte());
        return e;
    }

    /** Un paso en R o C (F21–F30, A5). */
    public static PasoEntity aEntidad(pe.mathnova.domain.factorizacion.Extension.Paso p, int orden) {
        PasoEntity e = new PasoEntity();
        e.setOrden(orden);
        e.setTecnicaCodigo(p.codigoTecnica());
        e.setJustificacion(p.justificacion());
        // antes_texto y antes_latex son NOT NULL en V1.
        e.setAntesTexto(p.antes() == null ? "" : pe.mathnova.domain.expresion.ImpresoraTexto.imprimir(p.antes()));
        e.setAntesLatex(p.antes() == null ? "" : ImpresoraLatex.imprimir(p.antes()));
        if (p.despues() != null) {
            e.setDespuesTexto(pe.mathnova.domain.expresion.ImpresoraTexto.imprimir(p.despues()));
            e.setDespuesLatex(ImpresoraLatex.imprimir(p.despues()));
        }
        if (p.trabajo() != null) {
            e.setTrabajoTipo(tipo(p.trabajo()));
            e.setTrabajoJson(json(p.trabajo()));
        }
        e.setEsDescartado(false);
        return e;
    }

    /**
     * Un paso dado como expresiones (P01, L01–L07, D01–D09). V1 exige "después" en un paso aplicado: si
     * no hay una expresión (una evaluación que da 0/0), va el último renglón del trabajo.
     */
    public static PasoEntity aEntidad(String codigo, String justificacion, pe.mathnova.domain.expresion.Expresion antes,
                                      pe.mathnova.domain.expresion.Expresion despues, TrabajoIntermedio trabajo, int orden) {
        PasoEntity e = new PasoEntity();
        e.setOrden(orden);
        e.setTecnicaCodigo(codigo);
        e.setJustificacion(justificacion);
        e.setAntesTexto(antes == null ? "" : pe.mathnova.domain.expresion.ImpresoraTexto.imprimir(antes));
        e.setAntesLatex(antes == null ? "" : ImpresoraLatex.imprimir(antes));
        if (despues != null) {
            e.setDespuesTexto(pe.mathnova.domain.expresion.ImpresoraTexto.imprimir(despues));
            e.setDespuesLatex(ImpresoraLatex.imprimir(despues));
        } else {
            String t = trabajo instanceof TrabajoIntermedio.Agrupacion a && !a.grupos().isEmpty()
                    ? a.grupos().getLast() : justificacion;
            e.setDespuesTexto(t);
            e.setDespuesLatex("\\text{" + t.replace("\\", "").replace("{", "").replace("}", "") + "}");
        }
        if (trabajo != null) {
            e.setTrabajoTipo(tipo(trabajo));
            e.setTrabajoJson(json(trabajo));
        }
        e.setEsDescartado(false);
        return e;
    }

    /** Valor de trabajo_tipo (CHECK de V1). */
    static String tipo(TrabajoIntermedio t) {
        return switch (t) {
            case TrabajoIntermedio.AspaSimple a -> "ASPA_SIMPLE";
            case TrabajoIntermedio.TablaRuffini r -> "TABLA_RUFFINI";
            case TrabajoIntermedio.AspaDobleEspecial a -> "ASPA_DOBLE_ESPECIAL";
            case TrabajoIntermedio.SistemaCoeficientes s -> "SISTEMA_COEFICIENTES";
            case TrabajoIntermedio.Sustitucion s -> "SUSTITUCION";
            case TrabajoIntermedio.Agrupacion a -> "OTRO";
            case TrabajoIntermedio.Comparacion c -> "OTRO";
            // V1 no tiene tipos propios para los trabajos del Sprint 7: van como OTRO con su JSON
            case TrabajoIntermedio.TablaPartes p -> "OTRO";
            case TrabajoIntermedio.TrianguloSustitucion ts -> "OTRO";
            case TrabajoIntermedio.AspaDoble a -> "OTRO";
            case TrabajoIntermedio.ArbolCadena a -> "OTRO";
        };
    }

    /** {"tipo":"AGRUPACION","grupos":[…]}, {"tipo":"ASPA_SIMPLE",…}, {"tipo":"TABLA_RUFFINI",…}… */
    static String json(TrabajoIntermedio t) {
        return switch (t) {
            case TrabajoIntermedio.Agrupacion a -> "{\"tipo\":\"AGRUPACION\",\"grupos\":" + lista(a.grupos()) + "}";
            case TrabajoIntermedio.AspaSimple a -> "{\"tipo\":\"ASPA_SIMPLE\",\"fila1\":" + texto(a.fila1())
                    + ",\"fila2\":" + texto(a.fila2()) + ",\"verificacion\":" + texto(a.verificacion())
                    + ",\"variable\":" + texto(String.valueOf(a.variable()))
                    + ",\"a1\":" + numero(a.a1()) + ",\"c1\":" + numero(a.c1())
                    + ",\"a2\":" + numero(a.a2()) + ",\"c2\":" + numero(a.c2()) + "}";
            case TrabajoIntermedio.Comparacion c -> "{\"tipo\":\"COMPARACION\",\"detalle\":" + texto(c.detalle()) + "}";
            case TrabajoIntermedio.TablaRuffini r -> "{\"tipo\":\"TABLA_RUFFINI\",\"variable\":"
                    + texto(String.valueOf(r.variable())) + ",\"candidatos\":["
                    + String.join(",", r.candidatos().stream().map(c -> "{\"valor\":" + numero(c.valor())
                            + ",\"residuo\":" + numero(c.residuo()) + "}").toList())
                    + "],\"divisiones\":["
                    + String.join(",", r.divisiones().stream().map(d -> "{\"raiz\":" + numero(d.raiz())
                            + ",\"coeficientes\":" + numeros(d.coeficientes())
                            + ",\"productos\":" + numeros(d.productos())
                            + ",\"resultados\":" + numeros(d.resultados())
                            + ",\"rellenos\":" + d.rellenos() + "}").toList())
                    + "]}";
            case TrabajoIntermedio.AspaDobleEspecial a -> "{\"tipo\":\"ASPA_DOBLE_ESPECIAL\",\"variable\":"
                    + texto(String.valueOf(a.variable())) + ",\"a1\":" + numero(a.a1()) + ",\"p\":" + numero(a.p())
                    + ",\"e1\":" + numero(a.e1()) + ",\"a2\":" + numero(a.a2()) + ",\"q\":" + numero(a.q())
                    + ",\"e2\":" + numero(a.e2()) + ",\"coeficienteX2\":" + numero(a.coeficienteX2()) + "}";
            case TrabajoIntermedio.SistemaCoeficientes s -> "{\"tipo\":\"SISTEMA_COEFICIENTES\",\"planteo\":"
                    + texto(s.planteo()) + ",\"ecuaciones\":["
                    + String.join(",", s.ecuaciones().stream().map(e -> "{\"potencia\":" + texto(e.potencia())
                            + ",\"ecuacion\":" + texto(e.ecuacion()) + "}").toList())
                    + "],\"solucion\":" + lista(s.solucion()) + ",\"resultado\":" + texto(s.resultado()) + "}";
            case TrabajoIntermedio.Sustitucion s -> "{\"tipo\":\"SUSTITUCION\",\"variableNueva\":"
                    + texto(s.variableNueva()) + ",\"definicion\":" + texto(s.definicion())
                    + ",\"sustituir\":" + texto(s.sustituir()) + ",\"factorizar\":" + texto(s.factorizar())
                    + ",\"regresar\":" + texto(s.regresar()) + "}";
            case TrabajoIntermedio.TablaPartes p -> "{\"tipo\":\"TABLA_PARTES\",\"u\":" + texto(p.u())
                    + ",\"du\":" + texto(p.du()) + ",\"dv\":" + texto(p.dv()) + ",\"v\":" + texto(p.v())
                    + ",\"ilate\":" + texto(p.ilate()) + ",\"lineas\":" + lista(p.lineas()) + "}";
            case TrabajoIntermedio.TrianguloSustitucion ts -> "{\"tipo\":\"TRIANGULO_SUSTITUCION\",\"sustitucion\":"
                    + texto(ts.sustitucion()) + ",\"opuesto\":" + texto(ts.opuesto()) + ",\"adyacente\":"
                    + texto(ts.adyacente()) + ",\"hipotenusa\":" + texto(ts.hipotenusa()) + ",\"angulo\":"
                    + texto(ts.angulo()) + ",\"razones\":" + lista(ts.razones()) + ",\"lineas\":" + lista(ts.lineas()) + "}";
            case TrabajoIntermedio.AspaDoble a -> "{\"tipo\":\"ASPA_DOBLE\",\"x\":" + texto(String.valueOf(a.x()))
                    + ",\"y\":" + texto(String.valueOf(a.y())) + ",\"a1\":" + numero(a.a1()) + ",\"c1\":"
                    + numero(a.c1()) + ",\"f1\":" + numero(a.f1()) + ",\"a2\":" + numero(a.a2()) + ",\"c2\":"
                    + numero(a.c2()) + ",\"f2\":" + numero(a.f2()) + "}";
            case TrabajoIntermedio.ArbolCadena a -> "{\"tipo\":\"ARBOL_CADENA\",\"funcion\":" + texto(a.funcion())
                    + ",\"intermedias\":" + lista(a.intermedias()) + ",\"parametro\":" + texto(a.parametro())
                    + ",\"ramasArriba\":" + lista(a.ramasArriba()) + ",\"ramasAbajo\":" + lista(a.ramasAbajo())
                    + ",\"suma\":" + texto(a.suma()) + "}";
        };
    }

    /** Un racional exacto como cadena ("-3/4"): nunca REAL (RN02). */
    private static String numero(Racional r) {
        return texto(r.toString());
    }

    private static String numeros(List<Racional> valores) {
        return "[" + String.join(",", valores.stream().map(PasoMapper::numero).toList()) + "]";
    }

    private static String lista(List<String> valores) {
        return "[" + String.join(",", valores.stream().map(PasoMapper::texto).toList()) + "]";
    }

    /** Cadena JSON con comillas, barras y caracteres de control escapados. */
    public static String texto(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
