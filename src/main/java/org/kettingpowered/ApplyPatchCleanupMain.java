package org.kettingpowered;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApplyPatchCleanupMain {
    public static void main(String[] args) {
        Set<String> srcFiles = Arrays.stream(args).skip(1).flatMap(arg -> {
            final Path src_path = Path.of(arg);
            try {
                FilePathReader reader = new FilePathReader();
                Files.walkFileTree(src_path, reader);
                return reader.pathList.stream()
                        .map(path -> src_path.relativize(path).toString())
                        .filter(path -> path.endsWith(".java"))
                        .map(path -> path.substring(0,path.lastIndexOf(".java")).replace(File.separatorChar, '.'));
            } catch (IOException e) {
                return Stream.empty();
            }
        }).collect(Collectors.toSet());

        final Path patch_path = Path.of(args[0]);
        System.out.printf("Seatching for patches in: %s", patch_path);
        try {
            Files.walkFileTree(patch_path, new PatchCleanup(srcFiles));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}