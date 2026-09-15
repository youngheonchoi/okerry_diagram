package com.okerry.okerry_diagram.git.controller;

import com.okerry.okerry_diagram.git.exception.GitCloneException;
import com.okerry.okerry_diagram.git.exception.InvalidRepositoryUrlException;
import com.okerry.okerry_diagram.git.service.RepositoryCloneService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RepositoryCloneController {

    private final RepositoryCloneService repositoryCloneService;

    public RepositoryCloneController(RepositoryCloneService repositoryCloneService) {
        this.repositoryCloneService = repositoryCloneService;
    }

    @PostMapping("/api/projects/analyze")
    public Map<String, Object> analyze(@RequestBody Map<String, Object> param) {
        Object repositoryUrl = param.get("repositoryUrl");
        repositoryCloneService.verifyClone(repositoryUrl instanceof String value ? value : null);

        return Map.of("status", "CLONED");
    }

    @ExceptionHandler(InvalidRepositoryUrlException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRepositoryUrl(InvalidRepositoryUrlException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(GitCloneException.class)
    public ResponseEntity<Map<String, Object>> handleGitCloneFailure(GitCloneException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", exception.getMessage()));
    }

}
