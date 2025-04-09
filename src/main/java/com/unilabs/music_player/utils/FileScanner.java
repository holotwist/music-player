package com.unilabs.music_player.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.unilabs.music_player.model.AudioFile;

// Clase que nos permite listar todos los archivos de una carpeta dada
public class FileScanner {
    public static List<AudioFile> scanFolder(String path) {
        File folder = new File(path);
        List<AudioFile> audioFiles = new ArrayList<>();
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isFile() && (file.getName().endsWith(".mp3") || file.getName().endsWith(".wav"))) {
                audioFiles.add(new AudioFile(file.getAbsolutePath()));
            }
        }
        return audioFiles;
    }
}