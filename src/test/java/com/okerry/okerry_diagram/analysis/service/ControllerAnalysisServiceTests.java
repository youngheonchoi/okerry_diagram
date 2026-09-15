package com.okerry.okerry_diagram.analysis.service;

import com.okerry.okerry_diagram.project.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ControllerAnalysisServiceTests {

    @Autowired
    private ControllerAnalysisService controllerAnalysisService;

    @Autowired
    private ProjectService projectService;

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesControllerEndpointsFromJavaSource() throws IOException {
        Path sourceDirectory = temporaryDirectory.resolve("src/main/java/com/example/user");
        Files.createDirectories(sourceDirectory);
        Files.writeString(sourceDirectory.resolve("UserController.java"), """
                package com.example.user;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/users")
                class UserController {
                    @GetMapping("/{id}")
                    String find() { return "user"; }

                    @PostMapping("/insert")
                    void insert() { }
                }
                """);

        Map<String, Object> result = controllerAnalysisService.analyze(
                "https://github.com/example/sample.git", temporaryDirectory);
        List<Map<String, Object>> controllers = projectService.controllers(
                ((Number) result.get("projectId")).longValue());

        assertEquals(1, result.get("controllerCount"));
        assertEquals(2, result.get("apiCount"));
        assertEquals("UserController", controllers.get(0).get("className"));
        assertEquals(2, ((List<?>) controllers.get(0).get("apis")).size());
    }

}
