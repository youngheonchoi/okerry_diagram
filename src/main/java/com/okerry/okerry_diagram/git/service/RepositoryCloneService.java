package com.okerry.okerry_diagram.git.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class RepositoryCloneService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryCloneService.class);

    private final RepositoryUrlValidator repositoryUrlValidator;
    private final WorkspaceService workspaceService;
    private final GitCloneService gitCloneService;

    public RepositoryCloneService(RepositoryUrlValidator repositoryUrlValidator,
                                  WorkspaceService workspaceService,
                                  GitCloneService gitCloneService) {
        this.repositoryUrlValidator = repositoryUrlValidator;
        this.workspaceService = workspaceService;
        this.gitCloneService = gitCloneService;
    }

    public void verifyClone(String repositoryUrl) {
        String validatedRepositoryUrl = repositoryUrlValidator.validate(repositoryUrl);
        Path workspace = workspaceService.create(validatedRepositoryUrl);

        try {
            gitCloneService.cloneRepository(validatedRepositoryUrl, workspace);
            log.info("Repository cloned to workspace: {}", workspace);
        } catch (RuntimeException exception) {
            log.warn("Repository clone failed; workspace retained for inspection: {}", workspace);
            throw exception;
        }
    }

}
