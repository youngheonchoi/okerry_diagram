package com.okerry.okerry_diagram.git.service;

import com.okerry.okerry_diagram.analysis.service.ControllerAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;

@Service
public class RepositoryCloneService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryCloneService.class);

    private final RepositoryUrlValidator repositoryUrlValidator;
    private final WorkspaceService workspaceService;
    private final GitCloneService gitCloneService;
    private final ControllerAnalysisService controllerAnalysisService;

    public RepositoryCloneService(RepositoryUrlValidator repositoryUrlValidator,
                                  WorkspaceService workspaceService,
                                  GitCloneService gitCloneService,
                                  ControllerAnalysisService controllerAnalysisService) {
        this.repositoryUrlValidator = repositoryUrlValidator;
        this.workspaceService = workspaceService;
        this.gitCloneService = gitCloneService;
        this.controllerAnalysisService = controllerAnalysisService;
    }

    public Map<String, Object> analyze(String repositoryUrl) {
        String validatedRepositoryUrl = repositoryUrlValidator.validate(repositoryUrl);
        Path workspace = workspaceService.create(validatedRepositoryUrl);

        try {
            gitCloneService.cloneRepository(validatedRepositoryUrl, workspace);
            log.info("Repository cloned to workspace: {}", workspace);
            return controllerAnalysisService.analyze(validatedRepositoryUrl, workspace);
        } catch (RuntimeException exception) {
            log.warn("Repository clone failed; workspace retained for inspection: {}", workspace);
            throw exception;
        }
    }

}
