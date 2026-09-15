package com.okerry.okerry_diagram.project.mapper;

import java.util.List;
import java.util.Map;

public interface ProjectMapper {

    int insertProject(Map<String, Object> param);

    Map<String, Object> selectProject(Map<String, Object> param);

    List<Map<String, Object>> selectControllerList(Map<String, Object> param);

}
