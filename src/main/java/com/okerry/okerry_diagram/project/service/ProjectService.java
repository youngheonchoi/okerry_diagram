package com.okerry.okerry_diagram.project.service;

import com.okerry.okerry_diagram.project.mapper.ProjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectService {

    private final ProjectMapper projectMapper;

    public ProjectService(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    public List<Map<String, Object>> controllers(long projectId) {
        List<Map<String, Object>> rows = projectMapper.selectControllerList(Map.of("projectId", projectId));
        Map<Object, Map<String, Object>> controllers = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            Map<String, Object> controller = controllers.computeIfAbsent(row.get("classId"), key -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("classId", row.get("classId"));
                value.put("className", row.get("className"));
                value.put("apis", new ArrayList<Map<String, Object>>());
                return value;
            });
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> apis = (List<Map<String, Object>>) controller.get("apis");
            apis.add(Map.of("methodId", row.get("methodId"), "httpMethod", row.get("httpMethod"),
                    "requestPath", row.get("requestPath"), "methodName", row.get("methodName")));
        }
        return new ArrayList<>(controllers.values());
    }

}
