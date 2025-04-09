package com.unilabs.music_player.model;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

public class Mp3Adapter implements AudioPlayer {
    private final MediaPlayerFactory factory;
    private final MediaPlayer mediaPlayer;
    private volatile boolean playing = false; // Track state

    public Mp3Adapter() {
        factory = new MediaPlayerFactory();
        mediaPlayer = factory.mediaPlayers().newMediaPlayer();

        // Add listener to update playing state
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void playing(MediaPlayer mediaPlayer) {
                playing = true;
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                playing = false;
            }

            @Override
            public void stopped(MediaPlayer mediaPlayer) {
                playing = false;
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                playing = false;
                System.err.println("VLCJ Error during playback");
            }
        });
    }

    @Override
    public void play(String filePath) {
        playing = false; // Reset state before playing
        mediaPlayer.media().play(filePath);
        // Playback is async, playing flag will be set by listener
    }

    @Override
    public void stop() {
        if (mediaPlayer.status().isPlaying() || playing) { // Check both internal status and our flag
            mediaPlayer.controls().stop();
        }
        playing = false;
    }

    @Override
    public long getDurationMillis() {
        return mediaPlayer.status().length(); // Returns duration in ms
    }

    @Override
    public long getCurrentPositionMillis() {
        // time() devuelve el tiempo actual en ms si se está reproduciendo, en caso contrario suele ser 0 o el último valor
        if (isPlaying()) {
            return mediaPlayer.status().time();
        }
        return 0; // O devolver la última posición conocida si es necesario
    }

    @Override
    public boolean isPlaying() {
        // Utilizar la bandera actualizada por eventos, ya que vlcj isPlaying() puede producir lag
        // Compruebe también el estado interno como alternativa
        return playing || mediaPlayer.status().isPlaying();
    }

    // Liberamos los recursos al terminar su ciclo de vida
    public void release() {
        mediaPlayer.release();
        factory.release();
    }
}