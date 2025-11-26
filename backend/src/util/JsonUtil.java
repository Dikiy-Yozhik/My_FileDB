package util;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class JsonUtil {
    
    public static String toJson(Object obj) {
        if (obj == null) return "null";
        
        // Добавляем обработку DTO объектов
        if (obj instanceof api.dto.ErrorResponse) {
            return errorResponseToJson((api.dto.ErrorResponse) obj);
        } else if (obj instanceof api.dto.SuccessResponse) {
            return successResponseToJson((api.dto.SuccessResponse<?>) obj);
        } else if (obj instanceof api.dto.EmployeeResponse) {
            return employeeResponseToJson((api.dto.EmployeeResponse) obj);
        } else if (obj instanceof List) {
            // Проверяем если это список EmployeeResponse
            List<?> list = (List<?>) obj;
            if (!list.isEmpty() && list.get(0) instanceof api.dto.EmployeeResponse) {
                List<api.dto.EmployeeResponse> employeeList = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof api.dto.EmployeeResponse) {
                        employeeList.add((api.dto.EmployeeResponse) item);
                    }
                }
                return employeeResponseListToJson(employeeList);
            }
            return listToJson(list);
        } else if (obj instanceof String) {
            return "\"" + escapeJsonString((String) obj) + "\"";
        } else if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        } else if (obj instanceof Map) {
            return mapToJson((Map<?, ?>) obj);
        } else {
            return "\"" + escapeJsonString(obj.toString()) + "\"";
        }
    }

    // Добавляем методы для сериализации EmployeeResponse
    private static String employeeResponseToJson(api.dto.EmployeeResponse employee) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(employee.getId()).append(",");
        sb.append("\"name\":\"").append(escapeJsonString(employee.getName())).append("\",");
        sb.append("\"department\":\"").append(escapeJsonString(employee.getDepartment())).append("\",");
        sb.append("\"position\":\"").append(escapeJsonString(employee.getPosition())).append("\",");
        sb.append("\"salary\":").append(employee.getSalary()).append(",");
        sb.append("\"hireDate\":\"").append(escapeJsonString(employee.getHireDate())).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String employeeResponseListToJson(List<api.dto.EmployeeResponse> employees) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (api.dto.EmployeeResponse employee : employees) {
            if (!first) sb.append(",");
            sb.append(employeeResponseToJson(employee));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    // Обновляем successResponseToJson для правильной обработки данных
    private static String successResponseToJson(api.dto.SuccessResponse<?> success) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":").append(success.isSuccess()).append(",");
        sb.append("\"message\":\"").append(escapeJsonString(success.getMessage())).append("\"");
        
        // Добавляем data если есть
        if (success.getData() != null) {
            sb.append(",\"data\":").append(toJson(success.getData()));
        }
        
        // Добавляем total если есть
        if (success.getTotal() != null) {
            sb.append(",\"total\":").append(success.getTotal());
        }
        
        // Добавляем deletedCount если есть
        if (success.getDeletedCount() != null) {
            sb.append(",\"deletedCount\":").append(success.getDeletedCount());
        }
        
        sb.append("}");
        return sb.toString();
    }

    // errorResponseToJson уже должен быть (добавьте если нет)
    private static String errorResponseToJson(api.dto.ErrorResponse error) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":").append(error.isSuccess()).append(",");
        sb.append("\"error\":\"").append(escapeJsonString(error.getError())).append("\",");
        sb.append("\"message\":\"").append(escapeJsonString(error.getMessage())).append("\"");
        
        // Добавляем details если есть
        if (error.getDetails() != null && !error.getDetails().isEmpty()) {
            sb.append(",\"details\":").append(mapToJson(error.getDetails()));
        }
        
        // Добавляем timestamp
        if (error.getTimestamp() != null) {
            sb.append(",\"timestamp\":\"").append(escapeJsonString(error.getTimestamp())).append("\"");
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    public static Map<String, Object> parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        json = json.trim();
        
        if (json.startsWith("{") && json.endsWith("}")) {
            return parseObject(json.substring(1, json.length() - 1));
        } else {
            throw new IllegalArgumentException("Invalid JSON object: " + json);
        }
    }
    
    private static String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJsonString(entry.getKey().toString())).append("\"");
            sb.append(":");
            sb.append(toJson(entry.getValue()));
            first = false;
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    private static String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        
        for (Object item : list) {
            if (!first) sb.append(",");
            sb.append(toJson(item));
            first = false;
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    private static Map<String, Object> parseObject(String json) {
        Map<String, Object> result = new HashMap<>();
        if (json.trim().isEmpty()) return result;
        
        String[] pairs = splitJsonPairs(json);
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replaceAll("^\"|\"$", "");
                String value = keyValue[1].trim();
                result.put(key, parseValue(value));
            }
        }
        
        return result;
    }
    
    private static Object parseValue(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1).replace("\\\"", "\"");
        } else if ("true".equals(value)) {
            return true;
        } else if ("false".equals(value)) {
            return false;
        } else if ("null".equals(value)) {
            return null;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e1) {
                try {
                    return Float.parseFloat(value);
                } catch (NumberFormatException e2) {
                    return value; // Возвращаем как строку
                }
            }
        }
    }
    
    private static String[] splitJsonPairs(String json) {
        List<String> pairs = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        
        for (char c : json.toCharArray()) {
            if (c == '{' || c == '[') depth++;
            if (c == '}' || c == ']') depth--;
            
            if (c == ',' && depth == 0) {
                pairs.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            pairs.add(current.toString());
        }
        
        return pairs.toArray(new String[0]);
    }
    
    private static String escapeJsonString(String str) {
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\b", "\\b")
                 .replace("\f", "\\f")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
}
