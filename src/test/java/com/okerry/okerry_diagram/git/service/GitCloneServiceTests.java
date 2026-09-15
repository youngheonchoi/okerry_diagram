package com.okerry.okerry_diagram.git.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GitCloneServiceTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shallowClonesRepositoryIntoWorkspace() throws IOException, InterruptedException {
        Path sourceRepository = temporaryDirectory.resolve("source");
        Path remoteRepository = temporaryDirectory.resolve("remote.git");
        Path workspace = temporaryDirectory.resolve("workspace");

        runGit("init", sourceRepository.toString());
        runGit("-C", sourceRepository.toString(), "config", "user.email", "test@example.com");
        runGit("-C", sourceRepository.toString(), "config", "user.name", "Test User");
        Files.writeString(sourceRepository.resolve("README.md"), "source");
        runGit("-C", sourceRepository.toString(), "add", "README.md");
        runGit("-C", sourceRepository.toString(), "commit", "-m", "initial commit");
        runGit("clone", "--bare", sourceRepository.toString(), remoteRepository.toString());

        new GitCloneService(10).cloneRepository(remoteRepository.toUri().toString(), workspace);

        assertTrue(Files.exists(workspace.resolve("README.md")));
        assertTrue(Files.exists(workspace.resolve(".git/shallow")));
    }

    private void runGit(String... arguments) throws IOException, InterruptedException {
        List<String> command = new java.util.ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();

        if (process.waitFor() != 0) {
            throw new IllegalStateException("Could not prepare local Git test repository.");
        }
    }

}
