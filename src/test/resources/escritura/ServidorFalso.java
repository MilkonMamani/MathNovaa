import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servidor falso para probar ProcesoInferencia (no reconoce nada): habla el mismo protocolo que
 * servidor_unimernet.py. Responde "img<n>" con n = largo de la imagen en base64.
 * Modos (primer argumento): normal, lento (500 ms por imagen), morir (sale al primer pedido), roto (no carga).
 */
public class ServidorFalso {
    public static void main(String[] args) throws Exception {
        String modo = args.length > 0 ? args[0] : "normal";
        if (modo.equals("roto")) {
            System.out.println("{\"listo\": false, \"error\": \"FileNotFoundError: falta el modelo\"}");
            return;
        }
        System.out.println("{\"listo\": true, \"carga_ms\": 5, \"version\": \"falso\"}");
        System.out.flush();
        Pattern id = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
        Pattern img = Pattern.compile("\"imagen\"\\s*:\\s*\"([^\"]*)\"");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String linea;
        while ((linea = in.readLine()) != null) {
            if (modo.equals("morir")) {
                System.exit(3);
            }
            if (modo.equals("lento")) {
                Thread.sleep(500);
            }
            Matcher m = id.matcher(linea);
            Matcher i = img.matcher(linea);
            String n = m.find() ? m.group(1) : "0";
            int largo = i.find() ? i.group(1).length() : -1;
            System.out.println("{\"id\": " + n + ", \"latex\": \"img" + largo + "\", \"ms\": 1}");
            System.out.flush();
        }
    }
}
