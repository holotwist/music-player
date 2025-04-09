package com.unilabs.music_player.controller;

import com.unilabs.music_player.model.*;
import com.unilabs.music_player.factory.AudioPlayerFactory;

public class PlayerController {
    private AudioPlayer currentPlayer;
    private AudioFile currentFile; // Seguimiento del archivo actual

    public void play(AudioFile file) {
        stopCurrent(); // Detener cualquier reproducción existente (evita multiples audios a la vez)

        this.currentFile = file;
        try {
            this.currentPlayer = AudioPlayerFactory.getPlayer(file.getFormat());
            if (this.currentPlayer != null) {
                currentPlayer.play(file.getPath());
            }
        } catch (UnsupportedOperationException e) {
            System.err.println(e.getMessage());
            this.currentPlayer = null; // Asegurarse de que no hay reproductor si el formato no es compatible
            this.currentFile = null;
        } catch (Exception e) {
            System.err.println("Error initializing player: " + e.getMessage());
            stopCurrent(); // Limpiar si init falló parcialmente
        }
    }

    public void stopCurrent() {
        if (currentPlayer != null) {
            currentPlayer.stop();
            // Si es Mp3Adapter, libera recursos, con release
            if (currentPlayer instanceof Mp3Adapter) {
                ((Mp3Adapter) currentPlayer).release();
            }
            currentPlayer = null;
        }
        currentFile = null;
    }

    public long getCurrentPositionMillis() {
        return (currentPlayer != null && currentPlayer.isPlaying()) ? currentPlayer.getCurrentPositionMillis() : 0;
    }

    public long getDurationMillis() {
        return (currentPlayer != null) ? currentPlayer.getDurationMillis() : 0;
    }

    public boolean isPlaying() {
        return (currentPlayer != null) && currentPlayer.isPlaying();
    }

    public AudioFile getCurrentFile() {
        return currentFile;
    }

    public void cleanup() {
        stopCurrent(); // Esto también llama a release
    }
}