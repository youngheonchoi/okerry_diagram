package com.okerry.okerry_diagram.project.controller;

import com.okerry.okerry_diagram.project.service.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/api/projects/{projectId}/controllers")
    public List<Map<String, Object>> controllers(@PathVariable long projectId) {
        return projectService.controllers(projectId);
    }

}
