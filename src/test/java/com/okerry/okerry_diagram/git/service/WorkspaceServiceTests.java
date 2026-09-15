package com.okerry.okerry_diagram.git.service;

import com.okerry.okerry_diagram.git.exception.GitCloneException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceServiceTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    void deletesWorkspaceAndItsContents() throws IOException {
        WorkspaceService workspaceService = new WorkspaceService(temporaryDirectory.toString());
        Path workspace = workspaceService.create("https://github.com/example/sample.git");
        Files.writeString(workspace.resolve("repository.txt"), "cloned source");

        workspaceService.delete(workspace);

        assertFalse(Files.exists(workspace));
    }

    @Test
    void incrementsWorkspaceSequenceForSameRepository() {
        WorkspaceService workspaceService = new WorkspaceService(temporaryDirectory.toString());

        Path firstWorkspace = workspaceService.create("https://github.com/example/sample.git");
        Path secondWorkspace = workspaceService.create("https://github.com/example/sample.git");

        assertTrue(firstWorkspace.endsWith("sample_1"));
        assertTrue(secondWorkspace.endsWith("sample_2"));
    }

    @Test
    void rejectsWorkspaceOutsideConfiguredRoot() {
        WorkspaceService workspaceService = new WorkspaceService(temporaryDirectory.toString());

        assertThrows(GitCloneException.class,
                () -> workspaceService.delete(Path.of(System.getProperty("java.io.tmpdir"), "outside-workspace")));
    }

}
