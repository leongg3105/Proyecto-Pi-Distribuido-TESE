import java.io.*;
import java.net.*;

public class CoordinadorPi {
    public static void main(String[] args) {
        int puerto = 5000;
        int totalWorkers = 3;
        
        // Estructuras para asegurar el orden correcto al final
        String[] resultadosUnidos = new String[totalWorkers];
        long[] tiemposWorkers = new long[totalWorkers];
        String[] nombresNodos = {"ubuntu", "debian", "alpine"};
        
        // Definición de rangos fijos por distribución
        int[][] rangos = {
            {0, 3333},    // Para Ubuntu (Posición 0)
            {3334, 6666},  // Para Debian (Posición 1)
            {6667, 9999}   // Para Alpine (Posición 2)
        };
        
        System.out.println("=== COORDINADOR CON IDENTIFICACIÓN CONCURRENTE (9999 DÍGITOS) ===");
        
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("Escuchando en 0.0.0.0:" + puerto + "...");
            
            long tiempoInicioGlobal = 0;

            for (int i = 0; i < totalWorkers; i++) {
                Socket socketWorker = serverSocket.accept();
                
                if (i == 0) {
                    tiempoInicioGlobal = System.currentTimeMillis();
                }
                
                DataInputStream entrada = new DataInputStream(socketWorker.getInputStream());
                DataOutputStream salida = new DataOutputStream(socketWorker.getOutputStream());
                
                // FASE DE IDENTIFICACIÓN: El Cerebro pregunta quién eres
                String tipoDistro = entrada.readUTF().toLowerCase().trim();
                System.out.println("[+] Nodo conectado desde " + socketWorker.getInetAddress() + " identificado como: " + tipoDistro);
                
                // Buscar qué índice le corresponde a esa distribución
                int indiceBloque = -1;
                for (int j = 0; j < nombresNodos.length; j++) {
                    if (tipoDistro.equals(nombresNodos[j])) {
                        indiceBloque = j;
                        break;
                    }
                }
                
                // Si se conecta una distro desconocida, le asignamos por defecto el índice del ciclo actual
                if (indiceBloque == -1) {
                    System.out.println("[!] Distro no reconocida. Asignando rango por orden de llegada.");
                    indiceBloque = i;
                }
                
                // Enviar el rango correcto basado en su identidad
                salida.writeInt(rangos[indiceBloque][0]);
                salida.writeInt(rangos[indiceBloque][1]);
                System.out.println("   -> Asignado rango [" + rangos[indiceBloque][0] + " a " + rangos[indiceBloque][1] + "] a " + tipoDistro);
                
                // Recibir métricas y guardar en la posición del arreglo CORRECTA
                tiemposWorkers[indiceBloque] = entrada.readLong();
                resultadosUnidos[indiceBloque] = entrada.readUTF(); 
                
                System.out.println("[✓] Bloque de " + tipoDistro + " recibido en la posición [" + indiceBloque + "].");
                
                socketWorker.close();
            }
            
            long tiempoFinGlobal = System.currentTimeMillis();
            long tiempoTotalSistema = tiempoFinGlobal - tiempoInicioGlobal;

            // Ensamblar el resultado final (Garantiza que siempre sea Bloque 0 + Bloque 1 + Bloque 2)
            System.out.println("\n=== Ensamblando bloques en orden estricto... ===");
            StringBuilder piFinal = new StringBuilder("3.");
            for (int i = 0; i < totalWorkers; i++) {
                if (resultadosUnidos[i] != null) {
                    piFinal.append(resultadosUnidos[i]);
                }
            }
            
            // Guardar archivo txt en tu Fedora
            try (FileWriter escritor = new FileWriter("pi_9999.txt")) {
                escritor.write(piFinal.toString());
                System.out.println("[⭐] ¡ÉXITO! Archivo 'pi_9999.txt' generado de forma asíncrona.");
            }
            
            // REPORTE DE TIEMPOS
            System.out.println("\n=================================================");
            System.out.println("        REPORTE DE TIEMPOS DE EJECUCIÓN          ");
            System.out.println("=================================================");
            for (int i = 0; i < totalWorkers; i++) {
                System.out.println("Worker " + nombresNodos[i].toUpperCase() + ": " + tiemposWorkers[i] + " ms");
            }
            System.out.println("-------------------------------------------------");
            System.out.println("Tiempo Total del Sistema Distribuido: " + tiempoTotalSistema + " ms");
            System.out.println("=================================================");
            
        } catch (IOException e) {
            System.err.println("[-] Error en el Coordinador: " + e.getMessage());
        }
    }
}
