package com.unilabs.music_player.model;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class WavAdapter implements AudioPlayer {
    private Clip clip;
    private volatile boolean playing = false; // Estado de la pista

    @Override
    public void play(String filePath) {
        stop(); // Detener el clip anterior, si existe
        playing = false; // Reiniciar estado

        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(filePath));
            clip = AudioSystem.getClip();

            // Añadir listener para actualizar el estado
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.START) {
                    playing = true;
                } else if (event.getType() == LineEvent.Type.STOP) {
                    // Comprobar si se ha detenido porque ha llegado al final o se ha detenido manualmente
                    if (clip != null && clip.getMicrosecondPosition() >= clip.getMicrosecondLength()) {
                        // Alcanzó final naturalmente
                        playing = false;
                        closeClip(); // Cerrar cuando finalice
                    } else {
                        // Detenido manualmente o debido a un error (normalmente es así)
                        playing = false;
                    }
                } else if (event.getType() == LineEvent.Type.CLOSE) {
                    playing = false; // Asegurar que el estado es falso cuando está cerrado
                }
            });

            clip.open(audioInputStream);
            clip.start(); // Iniciar la reproducción de forma asíncrona

            // La reproducción se hace en un hilo independiente gestionado por Java Sound API
            // No nos preocupamos de ella por ahora

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error playing WAV: " + e.getMessage());
            playing = false;
            closeClip(); // Limpiar si error
        }
    }

    @Override
    public void stop() {
        if (clip != null) {
            clip.stop(); // Para reproducción
            closeClip(); // Libera recursos
        }
        playing = false;
    }

    private void closeClip() {
        if (clip != null) {
            if (clip.isOpen()) {
                clip.close(); // Cierra la línea para liberar recursos
            }
            clip = null; // Permite al Recolector de Basura manipular
        }
    }


    @Override
    public long getDurationMillis() {
        // Devuelve microsegundos, convertir a milisegundos
        return (clip != null && clip.isOpen()) ? clip.getMicrosecondLength() / 1000 : 0;
    }

    @Override
    public long getCurrentPositionMillis() {
        // Devuelve microsegundos, convertir a milisegundos
        return (clip != null && clip.isOpen()) ? clip.getMicrosecondPosition() / 1000 : 0;
    }

    @Override
    public boolean isPlaying() {
        // Usar la bandera actualizada por eventos, comprobar clip.isRunning() también es posible, pero la bandera es más segura
        // Evita quejas de la JVM
        return playing && clip != null && clip.isRunning();
    }
}