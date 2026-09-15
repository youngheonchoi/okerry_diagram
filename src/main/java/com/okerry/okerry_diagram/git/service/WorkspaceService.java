package com.okerry.okerry_diagram.git.service;

import com.okerry.okerry_diagram.git.exception.GitCloneException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.net.URI;
import java.net.URISyntaxException;

@Service
public class WorkspaceService {

    private final Path workspaceRoot;

    public WorkspaceService(@Value("${spring-flow.workspace.root:${java.io.tmpdir}/spring-flow/workspaces}") String workspaceRoot) {
        this.workspaceRoot = Path.of(workspaceRoot).toAbsolutePath().normalize();
    }

    public Path create(String repositoryUrl) {
        try {
            Files.createDirectories(workspaceRoot);
            String repositoryName = extractRepositoryName(repositoryUrl);

            for (int sequence = 1; sequence < Integer.MAX_VALUE; sequence++) {
                Path workspace = workspaceRoot.resolve(repositoryName + "_" + sequence);

                try {
                    return Files.createDirectory(workspace);
                } catch (java.nio.file.FileAlreadyExistsException ignored) {
                    // Keep previous clone history and allocate the next sequence number.
                }
            }

            throw new GitCloneException("Could not allocate a workspace name.");
        } catch (IOException exception) {
            throw new GitCloneException("Could not create the analysis workspace.", exception);
        }
    }

    private String extractRepositoryName(String repositoryUrl) {
        try {
            String path = new URI(repositoryUrl).getPath();
            String name = path == null ? "" : path.substring(path.lastIndexOf('/') + 1);

            if (name.endsWith(".git")) {
                name = name.substring(0, name.length() - 4);
            }

            name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
            return name.isBlank() ? "repository" : name;
        } catch (URISyntaxException exception) {
            return "repository";
        }
    }

    public void delete(Path workspace) {
        if (workspace == null) {
            return;
        }

        Path target = workspace.toAbsolutePath().normalize();
        if (!target.startsWith(workspaceRoot) || target.equals(workspaceRoot)) {
            throw new GitCloneException("Invalid workspace path.");
        }

        try (var paths = Files.walk(target)) {
            paths.sorted(Comparator.reverseOrder()).forEach(this::deletePath);
        } catch (IOException exception) {
            throw new GitCloneException("Could not delete the analysis workspace.", exception);
        }
    }

    private void deletePath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new GitCloneException("Could not delete the analysis workspace.", exception);
        }
    }

}
