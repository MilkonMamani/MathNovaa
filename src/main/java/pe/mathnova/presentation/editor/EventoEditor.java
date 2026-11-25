package pe.mathnova.presentation.editor;

import javafx.event.Event;
import javafx.event.EventType;

public class EventoEditor extends Event {

    public static final EventType<EventoEditor> ANY = new EventType<>(Event.ANY, "EDITOR");
    public static final EventType<EventoEditor> CAMBIO = new EventType<>(ANY, "EDITOR_CAMBIO");
    public static final EventType<EventoEditor> ENTER = new EventType<>(ANY, "EDITOR_ENTER");

    private final String latex;

    public EventoEditor(EventType<EventoEditor> tipo, String latex) {
        super(tipo);
        this.latex = latex;
    }

    public String getLatex() {
        return latex;
    }
}
