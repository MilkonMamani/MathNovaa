package pe.mathnova.domain.escritura;

import java.util.ArrayList;
import java.util.List;

public record Muestra(String etiqueta, double ancho, double alto, List<Trazo> trazos) {

    public Muestra {
        etiqueta = etiqueta == null ? "" : etiqueta;
        trazos = List.copyOf(trazos);
    }

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
