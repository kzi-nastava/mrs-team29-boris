package com.example.backendspringboot.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DevelopmentProfileImageCleanerTest {
    @TempDir
    Path directory;

    @Test
    void removesStoredImagesButKeepsStorageDirectory() throws Exception {
        Files.writeString(directory.resolve("old.jpg"), "image");
        Path nested = Files.createDirectory(directory.resolve("nested"));
        Files.writeString(nested.resolve("pending.png"), "image");

        new DevelopmentProfileImageCleaner(directory).run(null);

        assertTrue(Files.isDirectory(directory));
        try (var files = Files.list(directory)) {
            assertEquals(0, files.count());
        }
    }
}
