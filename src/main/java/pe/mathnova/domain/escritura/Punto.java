package pe.mathnova.domain.escritura;

/**
 * Un punto de un trazo.
 *
 * @param x en píxeles, desde el borde izquierdo del lienzo
 * @param y en píxeles, desde el borde superior del lienzo
 * @param t milisegundos desde que empezó la muestra (el primer punto del primer trazo es 0)
 */
public record Punto(double x, double y, long t) {
}
