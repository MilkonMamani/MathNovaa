package pe.mathnova.domain.escritura;

import java.util.ArrayList;
import java.util.List;

/**
 * Una muestra de escritura a mano: los trazos tal como se dibujaron, el tamaño del lienzo en que se
 * capturaron (para reproducirlos a otra escala) y la etiqueta que puso el desarrollador. La etiqueta es la
 * respuesta correcta para probar un reconocedor; no la produce ningún reconocedor.
 *
 * @param etiqueta lo que se escribió, por ejemplo "2x"
 * @param ancho ancho del lienzo de captura, en píxeles
 * @param alto alto del lienzo de captura, en píxeles
 * @param trazos en el orden en que se dibujaron
 */
public record Muestra(String etiqueta, double ancho, double alto, List<Trazo> trazos) {

    public Muestra {
        etiqueta = etiqueta == null ? "" : etiqueta;
        trazos = List.copyOf(trazos);
    }

    /** Los mismos trazos multiplicados por {@code escala} (x, y y el lienzo; el tiempo no cambia). */
    public Muestra escalada(double escala) {
        List<Trazo> otros = new ArrayList<>(trazos.size());
        for (Trazo t : trazos) {
            List<Punto> puntos = new ArrayList<>(t.puntos().size());
            for (Punto p : t.puntos()) {
                puntos.add(new Punto(p.x() * escala, p.y() * escala, p.t()));
            }
            otros.add(new Trazo(puntos));
        }
        return new Muestra(etiqueta, ancho * escala, alto * escala, otros);
    }
}
