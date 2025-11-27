package api;

import api.controllers.DatabaseController;
import api.controllers.EmployeeController;
import util.JsonUtil;
import api.dto.UserSession;
import api.controllers.BackupController;
import api.controllers.ExportController;
import service.AuthService;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class HttpServer {
    private final Map<String, UserSession> sessions; // Хранилище сессий
    private final Map<String, String> tokenToSessionId; // Токен -> SessionId
    
    public HttpServer(int port) {
        this.port = port;
        this.databaseController = new DatabaseController();
        this.employeeController = new EmployeeController(databaseController);
        this.authService = new AuthService();
        this.sessions = new HashMap<>();
        this.tokenToSessionId = new HashMap<>(); // 🔥 НОВОЕ: хранилище токенов
        this.exportController = new ExportController(databaseController);
        this.backupController = new BackupController(databaseController);
    }
    
    // 🔥 ИЗМЕНЯЕМ: создаем токен вместо cookie
    private String createSession(UserSession userSession) {
        String sessionId = generateSessionId();
        String token = generateToken();
        sessions.put(sessionId, userSession);
        tokenToSessionId.put(token, sessionId); // 🔥 Связываем токен с сессией
        return token; // 🔥 Возвращаем токен, а не sessionId
    }
    
    private String generateToken() {
        return "token_" + java.util.UUID.randomUUID().toString();
    }
    
    // 🔥 НОВЫЙ МЕТОД: получение сессии по токену
    private UserSession getSessionByToken(String token) {
        String sessionId = tokenToSessionId.get(token);
        if (sessionId != null) {
            return sessions.get(sessionId);
        }
        return null;
    }
    
    // 🔥 ИЗМЕНЯЕМ: получаем сессию из заголовка Authorization
    private UserSession getUserSession(Map<String, String> headers) {
        String authHeader = headers.get("Authorization");
        System.out.println("🔑 Authorization header: " + authHeader);
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            System.out.println("🔍 Found token: " + token);
            UserSession session = getSessionByToken(token);
            if (session != null) {
                System.out.println("✅ Valid session found for: " + session.getUsername());
                return session;
            } else {
                System.out.println("❌ Invalid or expired token: " + token);
            }
        }
        
        System.out.println("👤 No valid token found, returning null");
        return null;
    }

    private void sendResponse(OutputStream out, String responseBody, UserSession userSession, String method, Map<String, String> headers) throws IOException {
        String allowOrigin = "http://localhost:3000";
        
        // 🔥 УБИРАЕМ эту логику - токен уже создан в handleLogin
        // String responseWithToken = responseBody;
        // if (userSession != null && !userSession.getUsername().equals("guest") && responseBody.contains("Login successful")) {
        //     // ... создание токена ...
        // }
        
        byte[] responseBytes = responseBody.getBytes("UTF-8"); // <- используем оригинальный responseBody
        
        String response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json; charset=utf-8\r\n" +
                        "Access-Control-Allow-Origin: " + allowOrigin + "\r\n" +
                        "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
                        "Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With\r\n" +
                        "Access-Control-Allow-Credentials: true\r\n" +
                        "Access-Control-Max-Age: 3600\r\n" +
                        "Content-Length: " + responseBytes.length + "\r\n" +
                        "\r\n";
        
        out.write(response.getBytes("UTF-8"));
        out.write(responseBytes);
        out.flush();
        System.out.println("✅ Response sent successfully! Length: " + responseBytes.length);
    }

    private boolean handleOptionsRequest(OutputStream out, Map<String, String> headers) throws IOException {
        // 🔥 ФИКСИРУЕМ ORIGIN ДЛЯ OPTIONS
        String allowOrigin = "http://localhost:3000";
        
        String response = "HTTP/1.1 200 OK\r\n" +
                "Access-Control-Allow-Origin: " + allowOrigin + "\r\n" +
                "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With\r\n" +
                "Access-Control-Allow-Credentials: true\r\n" + // 🔥 ДОБАВЛЯЕМ ЭТУ СТРОКУ
                "Access-Control-Max-Age: 3600\r\n" +
                "Content-Length: 0\r\n" +
                "\r\n";
        
        out.write(response.getBytes());
        out.flush();
        return true;
    }

    private void sendErrorResponse(OutputStream out, String errorBody) throws IOException {
        // 🔥 ДОБАВЛЯЕМ CORS В ОШИБОЧНЫЕ ОТВЕТЫ
        String response = "HTTP/1.1 500 Internal Server Error\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Access-Control-Allow-Origin: http://localhost:3000\r\n" +
                        "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
                        "Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With\r\n" +
                        "Access-Control-Allow-Credentials: true\r\n" +
                        "Content-Length: " + errorBody.length() + "\r\n" +
                        "\r\n" +
                        errorBody;
        
        out.write(response.getBytes());
        out.flush();
    }

    private final int port;
    private ServerSocket serverSocket;
    private boolean isRunning;
    private final DatabaseController databaseController;
    private final EmployeeController employeeController;
    private final AuthService authService;
    private final ExportController exportController;
    private final BackupController backupController;
    
    
    // private UserSession getSession(String sessionId) {
    //     return sessions.get(sessionId);
    // }
    
    private String generateSessionId() {
        return java.util.UUID.randomUUID().toString();
    }
    
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
            
            // 🔥 Обработка OPTIONS ДО чтения body
            if ("OPTIONS".equals(method)) {
                System.out.println("✅ Handling CORS preflight for: " + path);
                handleOptionsRequest(out, headers);
                return; // Завершаем обработку
            }
            
            // Read body if exists (только для НЕ-OPTIONS запросов)
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
            
            // Send response with proper CORS headers
            sendResponse(out, responseBody, userSession, method, headers);
            
        } catch (Exception e) {
            // Send error response with CORS headers
            String errorResponse = "{\"success\":false,\"error\":\"SERVER_ERROR\",\"message\":\"Internal server error\"}";
            sendErrorResponse(out, errorResponse);
        } finally {
            clientSocket.close();
        }
    }  

    // 🔥 Метод для проверки, требует ли endpoint авторизации
    private boolean requiresAuthentication(String path) {
        // Список endpoint'ов, доступных без авторизации
        String[] publicEndpoints = {"/auth/login", "/auth/status", "/frontend/"};
        
        for (String endpoint : publicEndpoints) {
            if (path.startsWith(endpoint)) {
                return false;
            }
        }
        return true; // все остальные endpoint'ы требуют авторизации
    }

    // 🔥 Метод для проверки, авторизован ли пользователь
    private boolean isAuthenticated(UserSession userSession) {
        return userSession != null && !userSession.getUsername().equals("guest");
    }
    
    // Обновляем processRequest для принятия userSession
    private String processRequest(String method, String path, String requestBody, 
                        Map<String, String> headers, UserSession userSession) {
        try {
            System.out.println("=== PROCESSING REQUEST ===");
            
            // 🔥 Показываем реальный статус аутентификации
            if (userSession != null) {
                System.out.println("User: " + userSession.getUsername() + " [" + userSession.getRole() + "]");
            } else {
                System.out.println("User: NOT AUTHENTICATED");
            }

            // 🔥 ОТЛАДОЧНАЯ ИНФОРМАЦИЯ
            System.out.println("🔑 Authorization: " + headers.get("Authorization"));
            System.out.println("👤 User session: " + (userSession != null ? userSession.getUsername() : "null"));
            
            // ПРОВЕРКА АВТОРИЗАЦИИ
            if (requiresAuthentication(path) && !isAuthenticated(userSession)) {
                System.out.println("🚫 Unauthorized access attempt to: " + path);
                return "{\"success\":false,\"error\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}";
            }
            
            System.out.println("Method: " + method);
            System.out.println("Path: " + path);
            System.out.println("Headers: " + headers);            

            if ("GET".equals(method) && path.startsWith("/frontend/")) {
                return serveStaticFile(path);
            }
            
            System.out.println("✅ Processing regular request: " + method + " " + path);
            
            // Аутентификация
            if (path.equals("/auth/login")) {
                String loginResult = handleLogin(requestBody);
                System.out.println("🔐 Login result: " + loginResult); // ← ДОБАВЬТЕ ЭТУ СТРОКУ
                return loginResult;
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
                case "/backup/create":
                    if ("POST".equals(method)) return backupController.createBackup(userSession);
                    break;
                    
                case "/backup/restore":
                    if ("POST".equals(method)) return backupController.restoreBackup(requestBody, userSession);
                    break;
                    
                case "/backup/list":
                    if ("GET".equals(method)) return backupController.listBackups(userSession);
                    break;
                    
                case "/backup/delete":
                    if ("DELETE".equals(method)) return backupController.deleteBackup(requestBody, userSession);
                    break;
                    
                case "/export/excel":
                    if ("GET".equals(method)) return exportController.exportToExcel(userSession);
                    break;
                    
                case "/export/list":
                    if ("GET".equals(method)) return exportController.listExportedFiles(userSession);
                    break;
                    
                case "/export/download":
                    if ("GET".equals(method)) {
                        String filePath = queryParams.get("file");
                        return serveFileDownload(filePath, userSession);
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
            System.out.println("Error in processRequest: " + e.getMessage());
            e.printStackTrace();
            return "{\"success\":false,\"error\":\"REQUEST_PROCESSING_ERROR\",\"message\":\"Error processing request: " + e.getMessage() + "\"}";
        }
    }

    private String serveFileDownload(String filePath, UserSession userSession) {
        try {
            if (!isAuthenticated(userSession)) {
                return "{\"success\":false,\"error\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}";
            }
            
            File file = new File(filePath);
            if (!file.exists()) {
                return "{\"success\":false,\"error\":\"FILE_NOT_FOUND\",\"message\":\"File not found: " + filePath + "\"}";
            }
        
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("filePath", filePath);
            fileInfo.put("fileName", file.getName());
            fileInfo.put("fileSize", file.length());
            fileInfo.put("downloadUrl", "file://" + file.getAbsolutePath());
            fileInfo.put("message", "Файл готов к скачиванию. Путь: " + file.getAbsolutePath());
            
            return "{\"success\":true,\"data\":" + JsonUtil.toJson(fileInfo) + "}";
            
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"DOWNLOAD_ERROR\",\"message\":\"Error serving file: " + e.getMessage() + "\"}";
        }
    }
    
    private String handleLogin(String requestBody) {
        try {
            System.out.println("🔐 Login attempt with body: " + requestBody);
            
            Map<String, Object> request = JsonUtil.parseJson(requestBody);
            String username = (String) request.get("username");
            String password = (String) request.get("password");
            
            System.out.println("👤 Authenticating user: " + username);
            
            UserSession userSession = authService.authenticate(username, password);
            if (userSession != null) {
                System.out.println("✅ Login successful for: " + username);
                
                // 🔥 СОЗДАЕМ СЕССИЮ И ПОЛУЧАЕМ ТОКЕН
                String token = createSession(userSession);
                System.out.println("🔑 Token created: " + token);
                
                // 🔥 ДОБАВЛЯЕМ ТОКЕН В ОТВЕТ!
                String response = "{" +
                    "\"success\":true," +
                    "\"message\":\"Login successful\"," +
                    "\"token\":\"" + token + "\"," +  // <- ДОБАВЬТЕ ЭТУ СТРОКУ
                    "\"data\":{" +
                        "\"username\":\"" + userSession.getUsername() + "\"," +
                        "\"role\":\"" + userSession.getRole().name() + "\"," +
                        "\"displayName\":\"" + userSession.getRole().getDisplayName() + "\"," +
                        "\"authenticated\":true" +
                    "}" +
                "}";
                
                System.out.println("📤 Final response with token: " + response);
                return response;
            } else {
                System.out.println("❌ Login failed for: " + username);
                return "{\"success\":false,\"error\":\"AUTH_FAILED\",\"message\":\"Invalid username or password\"}";
            }
        } catch (Exception e) {
            System.out.println("💥 Login error: " + e.getMessage());
            e.printStackTrace();
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
        try {
            System.out.println("🔍 Getting auth status for: " + (userSession != null ? userSession.getUsername() : "null"));
            
            Map<String, Object> data = new HashMap<>();
            
            if (userSession != null && !userSession.getUsername().equals("guest")) {
                // 🔥 ТОЛЬКО реальные пользователи считаются авторизованными
                data.put("username", userSession.getUsername());
                data.put("role", userSession.getRole().name());
                data.put("displayName", userSession.getRole().getDisplayName());
                data.put("authenticated", true);
            } else {
                // 🔥 Гости НЕ авторизованы
                data.put("username", "guest");
                data.put("role", "GUEST");
                data.put("displayName", "Гость");
                data.put("authenticated", false);
            }
            
            String response = "{\"success\":true,\"data\":" + JsonUtil.toJson(data) + "}";
            System.out.println("📤 Auth status response: " + response);
            return response;
            
        } catch (Exception e) {
            System.out.println("💥 Error in getAuthStatus: " + e.getMessage());
            return "{\"success\":false,\"error\":\"AUTH_STATUS_ERROR\",\"message\":\"Error getting auth status\"}";
        }
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
    
    

    private String serveStaticFile(String path) {
        try {
            // Убираем /frontend/ из пути
            String filePath = path.substring(10);
            if (filePath.isEmpty()) filePath = "index.html";
            
            File file = new File("frontend/" + filePath);
            if (!file.exists()) {
                return "HTTP/1.1 404 Not Found\r\n\r\nFile not found";
            }

            byte[] fileContent = Files.readAllBytes(file.toPath());
            String contentType = getContentType(filePath);
            
            return "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + fileContent.length + "\r\n" +
                "\r\n" +
                new String(fileContent);
        } catch (Exception e) {
            return "HTTP/1.1 500 Error\r\n\r\nError reading file";
        }
    }

    private String getContentType(String filePath) {
        if (filePath.endsWith(".html")) return "text/html";
        if (filePath.endsWith(".css")) return "text/css";
        if (filePath.endsWith(".js")) return "application/javascript";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg")) return "image/jpeg";
        return "text/plain";
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
