package com.example.backendspringboot.configuration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

@Component
@Profile("!test")
public class DevelopmentProfileImageCleaner implements ApplicationRunner {
    private final Path imageDirectory;

    public DevelopmentProfileImageCleaner() {
        this(Paths.get("uploads/profile-images"));
    }

    DevelopmentProfileImageCleaner(Path imageDirectory) {
        this.imageDirectory = imageDirectory;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Files.createDirectories(imageDirectory);
        try (var paths = Files.walk(imageDirectory)) {
            paths.filter(path -> !path.equals(imageDirectory))
                    .sorted(Comparator.reverseOrder())
                    .forEach(this::delete);
        }
    }

    private void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not clean profile image storage", exception);
        }
    }
}
