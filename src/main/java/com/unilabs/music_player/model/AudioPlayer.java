package com.unilabs.music_player.model;

public interface AudioPlayer {
    void play(String filePath);
    void stop();
    long getDurationMillis();
    long getCurrentPositionMillis();
    boolean isPlaying();
}