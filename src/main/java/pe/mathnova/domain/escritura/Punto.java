package pe.mathnova.domain.escritura;

/**
 * Un punto de un trazo.
 *
 * @param x en píxeles del lienzo
 * @param y en píxeles del lienzo
 * @param t en milisegundos (System.currentTimeMillis o el reloj del evento)
 */
public record Punto(double x, double y, long t) {
}
