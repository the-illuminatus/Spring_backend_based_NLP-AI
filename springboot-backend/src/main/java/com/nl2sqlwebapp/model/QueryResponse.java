package com.nl2sqlwebapp.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class QueryResponse {
    private boolean success;
    private String queryAnalysis;
    private String detectedIntent;
    private String baseTable;
    private List<String> selectColumns;
    private String whereConditions;
    private String generatedSql;
    private List<Map<String, String>> resultRows;
    private Integer rowsReturned;
    private String executionTime;
    private String errorMessage;
}

