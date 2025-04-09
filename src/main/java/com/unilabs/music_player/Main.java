package com.unilabs.music_player;

import com.unilabs.music_player.view.PlayerUI;

public class Main {
    public static void main(String[] args) throws Exception {
        // Para evitar que JNA haga puro spam en lanterna y borre la UI
        // Esto pasa porque no se ha puesto un modules-info.java, no se hace porque es un simple ejemplo
        // System.setOut(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));

        System.setErr(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));

        String path = args.length > 0 ? args[0] : "./music"; // Ruta predeterminada de la carpeta (Si no se especificó ninguna)

        // Crear el directorio si no existe
        java.io.File musicDir = new java.io.File(path);
        if (!musicDir.exists()) {
            System.out.println("Creating music directory: " + musicDir.getAbsolutePath());
            musicDir.mkdirs();
        } else if (!musicDir.isDirectory()) {
            System.err.println("Error: Provided path is not a directory: " + musicDir.getAbsolutePath());
            return; // Salir si la ruta es invalida
        }


        try {
            new PlayerUI().start(path);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native library error (likely VLC/JNA related): " + e.getMessage());
            System.err.println("Ensure VLC is installed and JNA can find its libraries.");
            System.err.println("Check system PATH or specify jna.library.path.");
        } catch (NoClassDefFoundError e) {
            System.err.println("Missing class error (check dependencies): " + e.getMessage());
            System.err.println("Ensure vlcj and its dependencies (jna, slf4j) are in the classpath.");
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace(); // Imprimir el stack trace para los errores genéricos
        } finally {
            System.out.println("Application finished.");
            // Forzar la salida si hilos daemon (demonio) impiden el apagado (por ejemplo, hilos VLC)
            System.exit(0);
        }
    }
}