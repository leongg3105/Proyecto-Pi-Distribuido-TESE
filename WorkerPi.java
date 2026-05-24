import java.io.*;
import java.net.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class WorkerPi {
    public static void main(String[] args) {
        String ipCoordinador = "10.137.109.71"; // IP de ZeroTier de Leo (Coordinador)
        int puerto = 5000;

        System.out.println("=== WORKER BBP HILOS DE PRECISIÓN (DOCKER) ===");
        
        try (
            Socket socket = new Socket(ipCoordinador, puerto);
            DataInputStream entrada = new DataInputStream(socket.getInputStream());
            DataOutputStream salida = new DataOutputStream(socket.getOutputStream())
        ) {
            // ============================================================
            // PASO CRUCIAL: IDENTIFICACIÓN DEL NODO
            // Aquí se debe poner "ubuntu", "debian" o "alpine" según el contenedor
            salida.writeUTF("ubuntu"); 
            // ============================================================

            System.out.println("[✓] Enlazado al servidor. Esperando rango...");

            // Recibir segmento de dígitos asignado por el Cerebro
            int inicio = entrada.readInt();
            int fin = entrada.readInt();
            System.out.println("[+] Calculando segmento de dígitos: [" + inicio + " a " + fin + "]");

            // INICIAR MEDICIÓN DE TIEMPO LOCAL (Objetivo de la práctica)
            long inicioCalculo = System.currentTimeMillis();
            
            String resultadoDigitos = calcularBloqueBBP(inicio, fin);
            
            long finCalculo = System.currentTimeMillis();
            long tiempoEjecucionLocal = finCalculo - inicioCalculo;

            // ENVIAR MÉTRICAS Y RESULTADOS AL CEREBRO
            salida.writeLong(tiempoEjecucionLocal);
            salida.writeUTF(resultadoDigitos);
            
            System.out.println("[✓] Cálculo terminado en " + tiempoEjecucionLocal + " ms. Envío completado.");

        } catch (IOException e) {
            System.err.println("[-] Error de comunicación: " + e.getMessage());
        }
    }

    // Algoritmo de alta precisión BBP con BigDecimal
    private static String calcularBloqueBBP(int inicio, int fin) {
        int precisionRequerida = fin - inicio + 1;
        int escalaInterna = precisionRequerida + 20; 
        BigDecimal sumaTotal = BigDecimal.ZERO.setScale(escalaInterna, RoundingMode.HALF_UP);

        for (int k = inicio; k <= fin; k++) {
            BigDecimal unOctavo = BigDecimal.valueOf(1).divide(BigDecimal.valueOf(8).pow(k), escalaInterna, RoundingMode.HALF_UP);
            
            BigDecimal t1 = BigDecimal.valueOf(4).divide(BigDecimal.valueOf(8L * k + 1), escalaInterna, RoundingMode.HALF_UP);
            BigDecimal t2 = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(8L * k + 4), escalaInterna, RoundingMode.HALF_UP);
            BigDecimal t3 = BigDecimal.valueOf(1).divide(BigDecimal.valueOf(8L * k + 5), escalaInterna, RoundingMode.HALF_UP);
            BigDecimal t4 = BigDecimal.valueOf(1).divide(BigDecimal.valueOf(8L * k + 6), escalaInterna, RoundingMode.HALF_UP);

            BigDecimal termino = t1.subtract(t2).subtract(t3).subtract(t4);
            sumaTotal = sumaTotal.add(unOctavo.multiply(termino));
        }

        String cadenaCompleta = sumaTotal.toString().substring(2); 
        if (cadenaCompleta.length() > precisionRequerida) {
            return cadenaCompleta.substring(0, precisionRequerida);
        }
        return cadenaCompleta;
    }
}
