package com.okerry.okerry_diagram.git.service;

import com.okerry.okerry_diagram.git.exception.GitCloneException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class GitCloneService {

    private final long timeoutSeconds;

    public GitCloneService(@Value("${spring-flow.git.clone-timeout-seconds:60}") long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public void cloneRepository(String repositoryUrl, Path workspace) {
        ProcessBuilder processBuilder = new ProcessBuilder(List.of(
                "git", "clone", "--depth", "1", "--", repositoryUrl, workspace.toString()
        ));
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        try {
            Process process = processBuilder.start();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new GitCloneException("Repository clone timed out.");
            }
            if (process.exitValue() != 0) {
                throw new GitCloneException("Repository clone failed. Confirm that it is a public Git repository.");
            }
        } catch (IOException exception) {
            throw new GitCloneException("Could not start Git clone.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GitCloneException("Repository clone was interrupted.", exception);
        }
    }

}
