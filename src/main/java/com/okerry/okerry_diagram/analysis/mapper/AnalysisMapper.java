package com.okerry.okerry_diagram.analysis.mapper;

import java.util.Map;

public interface AnalysisMapper {

    int insertSourceClass(Map<String, Object> param);

    int insertSourceMethod(Map<String, Object> param);

}
