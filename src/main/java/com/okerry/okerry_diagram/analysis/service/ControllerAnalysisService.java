package com.okerry.okerry_diagram.analysis.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.okerry.okerry_diagram.analysis.mapper.AnalysisMapper;
import com.okerry.okerry_diagram.project.mapper.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ControllerAnalysisService {

    private final ProjectMapper projectMapper;
    private final AnalysisMapper analysisMapper;

    public ControllerAnalysisService(ProjectMapper projectMapper, AnalysisMapper analysisMapper) {
        this.projectMapper = projectMapper;
        this.analysisMapper = analysisMapper;
    }

    @Transactional
    public Map<String, Object> analyze(String repositoryUrl, Path workspace) {
        Map<String, Object> project = new HashMap<>();
        project.put("name", repositoryName(repositoryUrl));
        project.put("repositoryUrl", repositoryUrl);
        project.put("status", "COMPLETED");
        projectMapper.insertProject(project);

        long projectId = ((Number) project.get("projectId")).longValue();
        int controllerCount = 0;
        int apiCount = 0;

        for (Path javaFile : findJavaFiles(workspace)) {
            try {
                CompilationUnit unit = StaticJavaParser.parse(javaFile);
                String packageName = unit.getPackageDeclaration().map(value -> value.getNameAsString()).orElse("");

                for (ClassOrInterfaceDeclaration type : unit.findAll(ClassOrInterfaceDeclaration.class)) {
                    if (!isController(type)) {
                        continue;
                    }

                    Map<String, Object> sourceClass = new HashMap<>();
                    sourceClass.put("projectId", projectId);
                    sourceClass.put("packageName", packageName);
                    sourceClass.put("className", type.getNameAsString());
                    sourceClass.put("qualifiedName", packageName.isBlank() ? type.getNameAsString() : packageName + "." + type.getNameAsString());
                    sourceClass.put("componentType", "CONTROLLER");
                    sourceClass.put("filePath", workspace.relativize(javaFile).toString());
                    analysisMapper.insertSourceClass(sourceClass);
                    controllerCount++;

                    List<String> classPaths = mappingPaths(type.getAnnotations());
                    for (MethodDeclaration method : type.getMethods()) {
                        List<Mapping> mappings = methodMappings(method.getAnnotations());
                        for (Mapping mapping : mappings) {
                            for (String classPath : classPaths) {
                                for (String methodPath : mapping.paths()) {
                                    Map<String, Object> sourceMethod = new HashMap<>();
                                    sourceMethod.put("classId", sourceClass.get("classId"));
                                    sourceMethod.put("methodName", method.getNameAsString());
                                    sourceMethod.put("httpMethod", mapping.httpMethod());
                                    sourceMethod.put("requestPath", joinPaths(classPath, methodPath));
                                    analysisMapper.insertSourceMethod(sourceMethod);
                                    apiCount++;
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // A malformed source file must not stop analysis of other Java files.
            }
        }

        return Map.of("projectId", projectId, "status", "COMPLETED", "controllerCount", controllerCount, "apiCount", apiCount);
    }

    private List<Path> findJavaFiles(Path workspace) {
        try (var paths = Files.walk(workspace)) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().replace('\\', '/').contains("/src/main/java/"))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not scan Java source files.", exception);
        }
    }

    private boolean isController(ClassOrInterfaceDeclaration type) {
        return type.getAnnotations().stream().map(annotation -> simpleName(annotation.getNameAsString()))
                .anyMatch(name -> name.equals("Controller") || name.equals("RestController"));
    }

    private List<String> mappingPaths(List<AnnotationExpr> annotations) {
        for (AnnotationExpr annotation : annotations) {
            if (simpleName(annotation.getNameAsString()).equals("RequestMapping")) {
                return annotationPaths(annotation);
            }
        }
        return List.of("");
    }

    private List<Mapping> methodMappings(List<AnnotationExpr> annotations) {
        List<Mapping> mappings = new ArrayList<>();
        for (AnnotationExpr annotation : annotations) {
            String name = simpleName(annotation.getNameAsString());
            String httpMethod = switch (name) {
                case "GetMapping" -> "GET";
                case "PostMapping" -> "POST";
                case "PutMapping" -> "PUT";
                case "PatchMapping" -> "PATCH";
                case "DeleteMapping" -> "DELETE";
                case "RequestMapping" -> "REQUEST";
                default -> null;
            };
            if (httpMethod != null) {
                mappings.add(new Mapping(httpMethod, annotationPaths(annotation)));
            }
        }
        return mappings;
    }

    private List<String> annotationPaths(AnnotationExpr annotation) {
        Expression value = null;
        if (annotation instanceof SingleMemberAnnotationExpr singleMember) {
            value = singleMember.getMemberValue();
        } else if (annotation instanceof NormalAnnotationExpr normal) {
            value = normal.getPairs().stream()
                    .filter(pair -> pair.getNameAsString().equals("value") || pair.getNameAsString().equals("path"))
                    .map(pair -> pair.getValue()).findFirst().orElse(null);
        }
        if (value instanceof ArrayInitializerExpr array) {
            return array.getValues().stream().map(this::stringValue).toList();
        }
        return List.of(value == null ? "" : stringValue(value));
    }

    private String stringValue(Expression expression) {
        String value = expression.toString();
        return value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")
                ? value.substring(1, value.length() - 1) : value;
    }

    private String joinPaths(String classPath, String methodPath) {
        String combined = (classPath + "/" + methodPath).replaceAll("/+", "/");
        return combined.startsWith("/") ? combined : "/" + combined;
    }

    private String repositoryName(String repositoryUrl) {
        String value = repositoryUrl.substring(repositoryUrl.lastIndexOf('/') + 1);
        return value.endsWith(".git") ? value.substring(0, value.length() - 4) : value;
    }

    private String simpleName(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }

    private record Mapping(String httpMethod, List<String> paths) {
    }

}
