package com.unilabs.music_player.factory;

import com.unilabs.music_player.model.*;

// Factory method para seleccionar el mejor adapter
public class AudioPlayerFactory {
    public static AudioPlayer getPlayer(String extension) {
        return switch (extension) {
            case "mp3" -> new Mp3Adapter();
            case "wav" -> new WavAdapter();
            default -> throw new UnsupportedOperationException("Unsupported format: " + extension);
        };
    }
}