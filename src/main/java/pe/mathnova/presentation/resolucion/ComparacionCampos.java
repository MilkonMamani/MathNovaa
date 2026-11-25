package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.presentation.latex.RenderizadorLatex;
import pe.mathnova.presentation.latex.VistaLatex;

public final class ComparacionCampos extends VBox {

    static final double ANCHO_ROTULO = 36;
    static final String VER_EXACTA = "Ver forma exacta";
    private static final double TAMANO = 16;
    private static final double TAMANO_MINIMO = 13;

    public record Fila(String rotulo, String latex, String descripcion, List<Desarrollo.Renglon> definiciones,
                       String exacto) {
        public Fila {
            definiciones = List.copyOf(definiciones);
        }
    }

    private final List<Fila> filas;
    private final List<Region> rotulos = new ArrayList<>();
    private final List<ToggleButton> controles = new ArrayList<>();
    private final RenderizadorLatex exacta = new RenderizadorLatex(14f);

    public ComparacionCampos(List<Fila> filas, Campo elegido) {
        super(14);
        this.filas = List.copyOf(filas);
        getStyleClass().add("comparacion-campos");
        setMinWidth(0);
        setFillWidth(true);
        for (Fila f : this.filas) {
            getChildren().add(fila(f, f.rotulo().equals(elegido == null ? "" : elegido.name())
                    || f.rotulo().contains("=")));
        }
    }

    public static List<Fila> filas(FactorizacionService.Comparacion c) {
        if (c.iguales()) {
            return List.of(new Fila("Q = R = C", c.latex().get(Campo.Q), c.nota(), List.of(), null));
        }
        List<Fila> l = new ArrayList<>();
        for (Campo campo : Campo.values()) {
            String latex = c.latex().get(campo);
            if (latex == null) {
                continue;
            }
            Resolucion r = c.resoluciones().get(campo);
            Optional<FormaLegible> legible = FormaLegible.de(r);
            String corta = legible.map(FormaLegible::latex).orElse(latex);
            l.add(new Fila(campo.name(), corta, DescripcionCampo.de(r, campo),
                    legible.map(FormaLegible::pie).orElse(List.of()), legible.isPresent() ? latex : null));
        }
        return l;
    }

    public static ComparacionCampos calculando() {
        List<Fila> l = new ArrayList<>();
        for (Campo c : Campo.values()) {
            l.add(new Fila(c.name(), null, "Calculando…", List.of(), null));
        }
        return new ComparacionCampos(l, null);
    }

    private Node fila(Fila f, boolean elegido) {
        Label rotulo = new Label(f.rotulo());
        rotulo.getStyleClass().add("comparacion-campo");
        if (elegido) {
            rotulo.getStyleClass().add("comparacion-campo-elegido");
        }
        double ancho = f.rotulo().length() > 1 ? Region.USE_PREF_SIZE : ANCHO_ROTULO;
        rotulo.setMinWidth(ancho);
        rotulo.setPrefWidth(ancho);
        rotulo.setMaxWidth(ancho);
        rotulo.setMinHeight(Region.USE_PREF_SIZE);
        rotulos.add(rotulo);

        VBox cuerpo = new VBox(4);
        cuerpo.getStyleClass().add("comparacion-cuerpo");
        cuerpo.setMinWidth(0);
        cuerpo.setPrefWidth(0);
        cuerpo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cuerpo, Priority.ALWAYS);
        if (f.latex() != null) {
            FormulaAjustable formula = new FormulaAjustable(TAMANO, TAMANO_MINIMO);
            formula.setRenglonesPrimero(true);
            formula.setLatex(f.latex());
            formula.getStyleClass().add("comparacion-formula");
            cuerpo.getChildren().add(formula);
        }
        if (f.descripcion() != null) {
            Label d = new Label(f.descripcion());
            d.setWrapText(true);
            d.setMinWidth(0);
            d.getStyleClass().add(f.latex() == null ? "comparacion-pendiente" : "comparacion-descripcion");
            cuerpo.getChildren().add(d);
        }
        if (!f.definiciones().isEmpty()) {
            VistaDesarrollo defs = new VistaDesarrollo(new Desarrollo(f.definiciones()), 14, 12);
            defs.getStyleClass().add("comparacion-definiciones");
            controles.addAll(defs.controles());
            cuerpo.getChildren().add(defs);
        }
        if (f.exacto() != null) {
            cuerpo.getChildren().add(exacto(f.exacto()));
        }
        HBox h = new HBox(12, rotulo, cuerpo);
        h.setAlignment(Pos.TOP_LEFT);
        h.setMinWidth(0);
        h.getStyleClass().add("comparacion-fila");
        return h;
    }

    private Node exacto(String latex) {
        ToggleButton ver = new ToggleButton(VER_EXACTA);
        ver.getStyleClass().add("pizarra-ver-detalle");
        ver.setMinWidth(Region.USE_PREF_SIZE);
        VistaLatex imagen = new VistaLatex(exacta, latex);
        StackPane marco = new StackPane(imagen);
        marco.setAlignment(Pos.TOP_LEFT);
        marco.getStyleClass().add("comparacion-exacta-contenido");
        ScrollPane desplazable = new ScrollPane(marco);
        desplazable.getStyleClass().add("comparacion-exacta");
        desplazable.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        desplazable.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        desplazable.setPannable(true);
        desplazable.setFitToHeight(true);
        desplazable.setMinWidth(0);
        desplazable.setPrefViewportWidth(1);
        desplazable.setMaxWidth(Double.MAX_VALUE);
        double alto = imagen.getBoundsInLocal().getHeight() + 22;
        desplazable.setPrefViewportHeight(alto);
        desplazable.setMinViewportHeight(alto);
        desplazable.setVisible(false);
        desplazable.setManaged(false);
        ver.selectedProperty().addListener((obs, antes, si) -> {
            ver.setText(si ? "Ocultar forma exacta" : VER_EXACTA);
            desplazable.setVisible(si);
            desplazable.setManaged(si);
        });
        controles.add(ver);
        VBox caja = new VBox(4, ver, desplazable);
        caja.setMinWidth(0);
        return caja;
    }

    public List<Fila> filas() {
        return filas;
    }

    public List<Region> rotulos() {
        return List.copyOf(rotulos);
    }

    public List<ToggleButton> controles() {
        return List.copyOf(controles);
    }
}
