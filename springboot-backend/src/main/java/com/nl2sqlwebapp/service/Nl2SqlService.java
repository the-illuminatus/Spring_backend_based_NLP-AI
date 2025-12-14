package com.nl2sqlwebapp.service;

import com.nl2sqlwebapp.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Nl2SqlService {

    @Value("${nl2sql.exe.path}")
    private String exePath;

    private ConnectionRequest currentConnection = null;

    public ConnectionResponse connect(ConnectionRequest request) {
        File exe = new File(exePath);
        if (!exe.exists()) {
            return new ConnectionResponse(false, "nl2sql.exe not found at " + exePath);
        }

        List<String> cmd = Arrays.asList(
                exe.getAbsolutePath(), request.getHost(), request.getUser(),
                request.getPassword(), request.getDatabase(), request.getPort()
        );
        try {
            Process process = new ProcessBuilder(cmd)
                .directory(exe.getParentFile())
                .redirectErrorStream(true)
                .start();

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                writer.write("exit\n");
                writer.flush();
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (line.contains("Enter your query")) break;
                }
                int exitCode = process.waitFor();
                if (exitCode == 0 || output.toString().contains("Enter your query")) {
                    currentConnection = request;
                    return new ConnectionResponse(true, "Connected to " + request.getDatabase() + " on " + request.getHost());
                } else {
                    return new ConnectionResponse(false, "Connection failed: " + output);
                }
            }
        } catch (Exception ex) {
            return new ConnectionResponse(false, "Connection error: " + ex.getMessage());
        }
    }

    public QueryResponse executeQuery(QueryRequest request) {
        if (currentConnection == null)
            return errorResponse("No active connection.");
        File exe = new File(exePath);
        if (!exe.exists())
            return errorResponse("nl2sql.exe not found at " + exePath);

        List<String> cmd = Arrays.asList(
                exe.getAbsolutePath(),
                currentConnection.getHost(), currentConnection.getUser(),
                currentConnection.getPassword(), currentConnection.getDatabase(),
                currentConnection.getPort()
        );
        try {
            Process process = new ProcessBuilder(cmd)
                .directory(exe.getParentFile())
                .redirectErrorStream(true)
                .start();

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                writer.write(request.getQuery() + "\n");
                writer.write("exit\n");
                writer.flush();
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                int exitCode = process.waitFor();
                if (exitCode == 0 || output.length() > 0) {
                    return parseNl2SqlOutput(output.toString());
                } else {
                    return errorResponse("Query execution failed: " + output);
                }
            }
        } catch (Exception ex) {
            return errorResponse("Query execution error: " + ex.getMessage());
        }
    }

    public boolean isConnected() {
        return currentConnection != null;
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("connected", isConnected());
        if (currentConnection != null) {
            Map<String, String> info = new HashMap<>();
            info.put("host", currentConnection.getHost());
            info.put("database", currentConnection.getDatabase());
            info.put("user", currentConnection.getUser());
            status.put("connectionInfo", info);
        }
        File exe = new File(exePath);
        status.put("nl2sqlExeFound", exe.exists());
        return status;
    }

    private QueryResponse errorResponse(String err) {
        QueryResponse response = new QueryResponse();
        response.setSuccess(false);
        response.setErrorMessage(err);
        return response;
    }

    private QueryResponse parseNl2SqlOutput(String output) {
        QueryResponse response = new QueryResponse();
        Map<String, Object> parsed = parseOutputSections(output);
        response.setSuccess(true);
        response.setQueryAnalysis((String) parsed.get("query_analysis"));
        response.setDetectedIntent((String) parsed.get("detected_intent"));
        response.setBaseTable((String) parsed.get("base_table"));
        response.setSelectColumns((List<String>) parsed.get("select_columns"));
        response.setWhereConditions((String) parsed.get("where_conditions"));
        response.setGeneratedSql((String) parsed.get("generated_sql"));
        response.setResultRows((List<Map<String, String>>) parsed.get("result_rows"));
        response.setRowsReturned((Integer) parsed.get("rows_returned"));
        response.setExecutionTime((String) parsed.get("execution_time"));
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseOutputSections(String output) {
        Map<String, Object> map = new HashMap<>();
        map.put("query_analysis", null);
        map.put("detected_intent", null);
        map.put("base_table", null);
        map.put("select_columns", new ArrayList<>());
        map.put("where_conditions", null);
        map.put("generated_sql", null);
        map.put("result_rows", new ArrayList<>());
        map.put("rows_returned", 0);
        map.put("execution_time", null);

        List<Map<String, String>> tableRows = new ArrayList<>();
        List<String> tableHeaders = new ArrayList<>();

        String[] lines = output.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("Original Query:"))
                map.put("query_analysis", line.replaceFirst("Original Query:", "").trim());
            else if (line.startsWith("Detected Intent:"))
                map.put("detected_intent", line.replaceFirst("Detected Intent:", "").trim());
            else if (line.startsWith("Base Table:"))
                map.put("base_table", line.replaceFirst("Base Table:", "").trim());
            else if (line.startsWith("Select Columns:")) {
                String cols = line.replaceFirst("Select Columns:", "").trim();
                if (!cols.isEmpty()) {
                    map.put("select_columns", Arrays.asList(cols.split("\\s*,\\s*")));
                }
            }
            else if (line.startsWith("Where Conditions:"))
                map.put("where_conditions", line.replaceFirst("Where Conditions:", "").trim());
            else if (line.startsWith("Generated SQL:"))
                map.put("generated_sql", line.replaceFirst("Generated SQL:", "").trim());
            else if (line.matches("\\d+ rows? returned")) {
                Pattern p = Pattern.compile("(\\d+) rows? returned");
                Matcher m = p.matcher(line);
                if (m.find())
                    map.put("rows_returned", Integer.parseInt(m.group(1)));
            }
            else if (line.startsWith("Execution time:"))
                map.put("execution_time", line.replaceFirst("Execution time:", "").trim());
            else if (line.contains("|") && !line.contains("---")) {
                String[] cols = line.split("\\|");
                List<String> cleaned = new ArrayList<>();
                for (String col : cols) {
                    if (!col.trim().isEmpty())
                        cleaned.add(col.trim());
                }
                if (tableHeaders.isEmpty()) {
                    tableHeaders.addAll(cleaned);
                } else if (cleaned.size() == tableHeaders.size()) {
                    Map<String, String> row = new HashMap<>();
                    for (int i = 0; i < tableHeaders.size(); i++) {
                        row.put(tableHeaders.get(i), cleaned.get(i));
                    }
                    tableRows.add(row);
                }
            }
        }
        map.put("result_rows", tableRows);
        return map;
    }
}

