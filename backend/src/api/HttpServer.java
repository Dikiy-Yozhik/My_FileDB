package api;

import api.controllers.DatabaseController;
import api.controllers.EmployeeController;
import util.JsonUtil;
import api.dto.UserSession;
import api.controllers.ExportController;
import service.AuthService;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;


public class HttpServer {
    private final int port;
    private ServerSocket serverSocket;
    private boolean isRunning;
    private final DatabaseController databaseController;
    private final EmployeeController employeeController;
    private final AuthService authService;
    private final Map<String, UserSession> sessions; // Хранилище сессий
    private final ExportController exportController;
    
    public HttpServer(int port) {
        this.port = port;
        this.databaseController = new DatabaseController();
        this.employeeController = new EmployeeController(databaseController);
        this.authService = new AuthService();
        this.sessions = new HashMap<>();
        this.exportController = new ExportController(databaseController);
    }
    
    // Добавляем методы для работы с сессиями
    private String createSession(UserSession userSession) {
        String sessionId = generateSessionId();
        sessions.put(sessionId, userSession);
        return sessionId;
    }
    
    private UserSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }
    
    private String generateSessionId() {
        return java.util.UUID.randomUUID().toString();
    }
    
    // В методе handleRequest добавляем обработку сессий
    private void handleRequest(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        OutputStream out = clientSocket.getOutputStream();
        
        try {
            // Read HTTP request
            String requestLine = in.readLine();
            if (requestLine == null) return;
            
            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 3) return;
            
            String method = requestParts[0];
            String path = requestParts[1];
            
            // Read headers
            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                String[] headerParts = line.split(":", 2);
                if (headerParts.length == 2) {
                    headers.put(headerParts[0].trim(), headerParts[1].trim());
                }
            }
            
            // Read body if exists
            String requestBody = null;
            if (headers.containsKey("Content-Length")) {
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                char[] bodyChars = new char[contentLength];
                in.read(bodyChars, 0, contentLength);
                requestBody = new String(bodyChars);
            }
            
            // Получаем сессию из cookies или создаем гостевую
            UserSession userSession = getUserSession(headers);
            
            // Process request with session
            String responseBody = processRequest(method, path, requestBody, headers, userSession);
            
            // Send response with session cookie if needed
            sendResponse(out, responseBody, userSession);
            
        } catch (Exception e) {
            // Request processing error
            String errorResponse = "HTTP/1.1 500 Internal Server Error\r\n" +
                                 "Content-Type: application/json\r\n" +
                                 "\r\n" +
                                 "{\"success\":false,\"error\":\"SERVER_ERROR\",\"message\":\"Internal server error\"}";
            out.write(errorResponse.getBytes());
        }
    }
    
    private UserSession getUserSession(Map<String, String> headers) {
        String cookieHeader = headers.get("Cookie");
        if (cookieHeader != null) {
            // Ищем sessionId в cookies
            for (String cookie : cookieHeader.split(";")) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && "sessionId".equals(parts[0])) {
                    UserSession session = getSession(parts[1]);
                    if (session != null) {
                        return session;
                    }
                }
            }
        }
        
        // Если сессии нет, создаем гостевую
        return authService.getGuestSession();
    }
    
    private void sendResponse(OutputStream out, String responseBody, UserSession userSession) throws IOException {
        String sessionCookie = "";
        
        // Если это новая сессия (не гостевая), устанавливаем cookie
        if (!userSession.getUsername().equals("guest")) {
            String sessionId = createSession(userSession);
            sessionCookie = "Set-Cookie: sessionId=" + sessionId + "; Path=/; HttpOnly\r\n";
        }
        
        String response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
                        "Access-Control-Allow-Headers: Content-Type, Cookie\r\n" +
                        "Access-Control-Allow-Credentials: true\r\n" +
                        sessionCookie +
                        "Content-Length: " + responseBody.length() + "\r\n" +
                        "\r\n" +
                        responseBody;
        
        out.write(response.getBytes());
        out.flush();
    }
    
    // Обновляем processRequest для принятия userSession
    private String processRequest(String method, String path, String requestBody, 
                                Map<String, String> headers, UserSession userSession) {
        try {
            System.out.println("=== PROCESSING REQUEST ===");
            System.out.println("User: " + userSession.getUsername() + " [" + userSession.getRole() + "]");
            System.out.println("Method: " + method);
            System.out.println("Path: " + path);
            
            // CORS preflight
            if ("OPTIONS".equals(method)) {
                return "{\"success\":true}";
            }
            
            // Аутентификация
            if (path.equals("/auth/login")) {
                return handleLogin(requestBody);
            }
            
            if (path.equals("/auth/logout")) {
                return handleLogout(headers);
            }
            
            if (path.equals("/auth/status")) {
                return getAuthStatus(userSession);
            }
            
            String[] pathParts = path.split("\\?");
            String endpoint = pathParts[0];
            Map<String, String> queryParams = parseQueryParams(pathParts.length > 1 ? pathParts[1] : "");
            
            // Routing с передачей userSession
            switch (endpoint) {
                case "/export/excel":
                    if ("GET".equals(method)) return exportController.exportToExcel(userSession);
                    break;
                    
                case "/export/list":
                    if ("GET".equals(method)) return exportController.listExportedFiles(userSession);
                    break;
                    
                case "/export/download":
                    if ("GET".equals(method)) {
                        String filePath = queryParams.get("file");
                        return exportController.downloadExcel(filePath, userSession);
                    }
                    break;

                case "/employees":
                    return handleEmployeesEndpoint(method, queryParams, requestBody, userSession);
                    
                case "/database/create":
                    if ("POST".equals(method)) return databaseController.createDatabase(requestBody, userSession);
                    break;
                    
                case "/database/load":
                    if ("POST".equals(method)) return databaseController.loadDatabase(requestBody, userSession);
                    break;
                    
                case "/database/info":
                    if ("GET".equals(method)) return databaseController.getDatabaseInfo(userSession);
                    break;
                    
                case "/database/backup":
                    if ("POST".equals(method)) return databaseController.backupDatabase(userSession);
                    break;
                    
                case "/database/clear":
                    if ("DELETE".equals(method)) return databaseController.clearDatabase(userSession);
                    break;
                    
                default:
                    if (endpoint.startsWith("/employees/")) {
                        String idParam = endpoint.substring("/employees/".length());
                        return handleEmployeeByIdEndpoint(method, idParam, requestBody, userSession);
                    }
            }
            
            return "{\"success\":false,\"error\":\"ENDPOINT_NOT_FOUND\",\"message\":\"Endpoint not found: " + endpoint + "\"}";
            
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"REQUEST_PROCESSING_ERROR\",\"message\":\"Error processing request: " + e.getMessage() + "\"}";
        }
    }
    
    // Обработчики аутентификации
    private String handleLogin(String requestBody) {
        try {
            Map<String, Object> request = JsonUtil.parseJson(requestBody);
            String username = (String) request.get("username");
            String password = (String) request.get("password");
            
            UserSession userSession = authService.authenticate(username, password);
            if (userSession != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("username", userSession.getUsername());
                data.put("role", userSession.getRole().name());
                data.put("displayName", userSession.getRole().getDisplayName());
                
                return "{\"success\":true,\"message\":\"Login successful\",\"data\":" + JsonUtil.toJson(data) + "}";
            } else {
                return "{\"success\":false,\"error\":\"AUTH_FAILED\",\"message\":\"Invalid username or password\"}";
            }
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"LOGIN_ERROR\",\"message\":\"Error during login: " + e.getMessage() + "\"}";
        }
    }
    
    private String handleLogout(Map<String, String> headers) {
        // Удаляем сессию из хранилища
        String cookieHeader = headers.get("Cookie");
        if (cookieHeader != null) {
            for (String cookie : cookieHeader.split(";")) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && "sessionId".equals(parts[0])) {
                    sessions.remove(parts[1]);
                    break;
                }
            }
        }
        return "{\"success\":true,\"message\":\"Logout successful\"}";
    }
    
    private String getAuthStatus(UserSession userSession) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", userSession.getUsername());
        data.put("role", userSession.getRole().name());
        data.put("displayName", userSession.getRole().getDisplayName());
        data.put("authenticated", userSession.isAuthenticated());
        
        return "{\"success\":true,\"data\":" + JsonUtil.toJson(data) + "}";
    }
    
    // Обновляем обработчики endpoint'ов для передачи userSession
    private String handleEmployeesEndpoint(String method, Map<String, String> queryParams, String requestBody, UserSession userSession) {
        switch (method) {
            case "GET":
                if (!queryParams.isEmpty()) {
                    return employeeController.searchEmployees(queryParams, userSession);
                } else {
                    return employeeController.getAllEmployees(userSession);
                }
                
            case "POST":
                return employeeController.createEmployee(requestBody, userSession);
                
            case "DELETE":
                return employeeController.deleteEmployeesByCriteria(queryParams, userSession);
                
            default:
                return "{\"success\":false,\"error\":\"METHOD_NOT_ALLOWED\",\"message\":\"Method not allowed for /employees\"}";
        }
    }
    
    private String handleEmployeeByIdEndpoint(String method, String idParam, String requestBody, UserSession userSession) {
        switch (method) {
            case "GET":
                return employeeController.getEmployeeById(idParam, userSession);
                
            case "PUT":
                return employeeController.updateEmployee(idParam, requestBody, userSession);
                
            case "DELETE":
                return employeeController.deleteEmployee(idParam, userSession);
                
            default:
                return "{\"success\":false,\"error\":\"METHOD_NOT_ALLOWED\",\"message\":\"Method not allowed for /employees/{id}\"}";
        }
    }
    
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        isRunning = true;
        
        System.out.println("=== My_FileDB HTTP Server ===");
        System.out.println("Server started on port " + port);
        System.out.println("Available endpoints:");
        System.out.println("  GET  /employees");
        System.out.println("  POST /employees");
        System.out.println("  GET  /employees/{id}");
        System.out.println("  PUT  /employees/{id}");
        System.out.println("  DELETE /employees/{id}");
        System.out.println("  GET  /employees/search");
        System.out.println("  DELETE /employees");
        System.out.println("  POST /database/create");
        System.out.println("  POST /database/load");
        System.out.println("  GET  /database/info");
        System.out.println("  POST /database/backup");
        System.out.println("  DELETE /database/clear");
        System.out.println("==============================");
        
        while (isRunning) {
            try (Socket clientSocket = serverSocket.accept()) {
                handleRequest(clientSocket);
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Error handling request: " + e.getMessage());
                }
            }
        }
    }
    
    public void stop() throws IOException {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        System.out.println("Server stopped");
    }
    
    private Map<String, String> parseQueryParams(String queryString) {
        Map<String, String> params = new HashMap<>();
        if (queryString == null || queryString.isEmpty()) return params;
        
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                try {
                    String key = URLDecoder.decode(keyValue[0], "UTF-8");
                    String value = URLDecoder.decode(keyValue[1], "UTF-8");
                    params.put(key, value);
                } catch (UnsupportedEncodingException e) {
                    // Ignore invalid parameters
                }
            }
        }
        return params;
    }
    
    public static void main(String[] args) {
        try {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
            HttpServer server = new HttpServer(port);
            server.start();
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
