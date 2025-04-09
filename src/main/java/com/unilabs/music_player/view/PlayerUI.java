package com.unilabs.music_player.view;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.unilabs.music_player.controller.PlayerController;
import com.unilabs.music_player.model.AudioFile;
import com.unilabs.music_player.utils.FileScanner;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PlayerUI {

    private final PlayerController controller;
    private Label nowPlaying;
    private Label progress;
    private MultiWindowTextGUI gui;
    private volatile boolean running = true; // Bandera para detener el hilo updater
    private Thread progressUpdaterThread;

    public PlayerUI() {
        this.controller = new PlayerController();
    }

    public void start(String path) throws IOException {
        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
        Screen screen = null;
        try {
            screen = terminalFactory.createScreen();
            screen.startScreen();

            gui = new MultiWindowTextGUI(screen); // Asignar a campo
            BasicWindow window = new BasicWindow("FNLY Player v0.0.1");




            Panel mainPanel = new Panel(new GridLayout(2));
            Panel left = new Panel();
            Panel right = new Panel();

            List<AudioFile> files = FileScanner.scanFolder(path);

            // Inicializar labels como campos
            nowPlaying = new Label("Now Playing: ");
            progress = new Label("00:00 - 00:00 : 0% played");
            Label details = new Label("Format | Frequency | Bitrate"); // TODO: Que esto sirva para algo
            Label footer = new Label("Ctlr+C to Exit");

            ActionListBox listBox = new ActionListBox(new TerminalSize(40, 20));
            for (AudioFile file : files) {
                // Mostrar solo el nombre de archivo en el listbox para abreviar y facilitar lectura
                String fileName = file.getPath().substring(file.getPath().lastIndexOf(File.separator) + 1);
                listBox.addItem(fileName, () -> {
                    // Actualizar inmediatamente la etiqueta con el nombre del archivo
                    nowPlaying.setText("Now Playing: " + fileName);
                    // Restablecer visualmente la barra de progreso
                    progress.setText("Loading...");
                    // Iniciar la reproducción en un nuevo subproceso para mantener IU responsiva
                    new Thread(() -> {
                        controller.play(file);
                        // Si la reproducción falla al instante, el progress updater se encargará del estado
                    }).start();
                });
            }

            left.addComponent(new Label("Songs:"));
            left.addComponent(listBox);

            right.addComponent(nowPlaying);
            right.addComponent(progress);
            right.addComponent(details);
            right.addComponent(new EmptySpace()); // Añadir algo de espacio
            right.addComponent(footer);

            mainPanel.addComponent(left.withBorder(Borders.singleLine("Playlist")));
            mainPanel.addComponent(right.withBorder(Borders.singleLine("Player")));

            window.setComponent(mainPanel);

            // --- Iniciar Hilo Progress Updater ---
            startProgressUpdater();

            // --- Mostrar Ventana ---
            gui.addWindowAndWait(window); // Bloquea hasta que se cierra la ventana

        } catch (Exception e) { // Captura de excepciones más representativas durante la configuración
            System.err.println("UI Initialization Error: " + e.getMessage());
            e.printStackTrace(); // Hay mejores opciones a esto, pero no quiero complicarlo lmao
        } finally {
            // --- Limpieza ---
            running = false; // Asegurarse de que la bandera está configurada
            controller.cleanup(); // Intento final de limpieza
            if (progressUpdaterThread != null) {
                progressUpdaterThread.interrupt(); // Interrumpir si de alguna manera sigue vivo
            }
            if (screen != null) {
                try {
                    screen.stopScreen(); // Asegurarse de que Screen realmente se detuvo
                } catch (IOException e) {
                    System.err.println("Error stopping screen: " + e.getMessage());
                }
            }
        }
    }

    private void startProgressUpdater() {
        progressUpdaterThread = new Thread(() -> {
            while (running) {
                try {
                    updateProgressLabel();
                    Thread.sleep(500); // Intervalo de actualización: 0.5 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Volver a interrumpir el hilo
                    running = false; // Salir del bucle si se ha interrumpido
                } catch (Exception e) {
                    // Evita crash en el hilo updater por problemas menores de la UI
                    System.err.println("Error in progress updater: " + e.getMessage());
                }
            }
            System.out.println("Progress updater thread finished.");
        });
        progressUpdaterThread.setDaemon(true); // Permitir que la JVM salga si este es el único hilo que queda
        progressUpdaterThread.start();
    }

    // Actualizar el texto de la etiqueta (se ejecuta en el hilo updater)
    private void updateProgressLabel() {
        if (controller.isPlaying()) {
            long current = controller.getCurrentPositionMillis();
            long total = controller.getDurationMillis();

            if (total > 0) {
                String currentTimeStr = formatTime(current);
                String totalTimeStr = formatTime(total);
                long percentage = (current * 100) / total;

                // Actualización directa - Lanterna quizá puede manejarlo razonablemente para las etiquetas
                // Lo anterior quizá produzca errores o flickering, si eso pasa, mejor refactorizar a:
                // gui.getGUIThread().invokeLater(() -> ... );
                final String progressText = String.format("%s - %s : %d%% played", currentTimeStr, totalTimeStr, percentage);
                if (gui != null) { // Ensure GUI object exists
                    // Programar la actualización en el hilo GUI por seguridad
                    gui.getGUIThread().invokeLater(() -> {
                        if (progress != null) { // Comprobar si la etiqueta sigue existiendo
                            progress.setText(progressText);
                        }
                    });
                }

            } else if (total == -1 && controller.getCurrentFile() != null) {
                // VLC podría devolver -1 para la duración inicialmente o para los streams
                String currentTimeStr = formatTime(current);
                final String progressText = String.format("%s - --:-- : Streaming?", currentTimeStr);
                if (gui != null) {
                    gui.getGUIThread().invokeLater(() -> {
                        if (progress != null) {
                            progress.setText(progressText);
                        }
                    });
                }
            } else {
                // Aún cargando o duración no disponible
                if (gui != null) {
                    gui.getGUIThread().invokeLater(() -> {
                        if (progress != null) {
                            if (controller.getCurrentFile() != null) { // Mostrar "Loading..." solo si se selecciona un archivo
                                progress.setText("Loading...");
                            } else {
                                progress.setText("00:00 - 00:00 : 0% played");
                            }
                        }
                    });
                }
            }
        } else {
            // Si no se está reproduciendo, muestra el estado idle solo si no hay ningún archivo seleccionado o la reproducción ha finalizado.
            if (controller.getCurrentFile() == null) {
                if (gui != null) {
                    gui.getGUIThread().invokeLater(() -> {
                        if (progress != null) {
                            progress.setText("00:00 - 00:00 : 0% played");
                        }
                    });
                }
            }
            // Si un archivo *estaba* reproduciéndose y se detiene, ¿conserva el último estado o se reinicia?
            // Reiniciemos por simplicidad. Y evitar errores tempranos.
            else if (progress != null && !progress.getText().startsWith("00:00 - 00:00")) {
                // Reiniciar sólo si aún no se ha reiniciado
                if (gui != null) {
                    gui.getGUIThread().invokeLater(() -> {
                        if (progress != null) {
                            progress.setText("00:00 - 00:00 : 0% played");
                        }
                    });
                }
            }
        }
    }

    // Método auxiliar para formatear milisegundos en MM:SS
    private String formatTime(long millis) {
        if (millis < 0) millis = 0; // Manejar los posibles valores negativos de las librerias
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        // long hours = (millis / (1000 * 60 * 60)) % 24; // Se pueden añadir horas si es necesario, por ahora nope
        return String.format("%02d:%02d", minutes, seconds);
    }
}