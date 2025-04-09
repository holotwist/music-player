package com.unilabs.music_player.model;

public class AudioFile {
    private String path;
    private String format;

    public AudioFile(String path) {
        this.path = path;
        this.format = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
    }

    public String getPath() { return path; }
    public String getFormat() { return format; }
}