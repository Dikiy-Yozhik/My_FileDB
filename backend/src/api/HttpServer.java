package api;

import api.controllers.DatabaseController;
import api.controllers.EmployeeController;
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
    
    public HttpServer(int port) {
        this.port = port;
        this.databaseController = new DatabaseController();
        this.employeeController = new EmployeeController(databaseController);
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
            
            // Process request
            String responseBody = processRequest(method, path, requestBody, headers);
            
            // Send response
            String response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/json\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
                            "Access-Control-Allow-Headers: Content-Type\r\n" +
                            "Content-Length: " + responseBody.length() + "\r\n" +
                            "\r\n" +
                            responseBody;
            
            out.write(response.getBytes());
            out.flush();
            
        } catch (Exception e) {
            // Request processing error
            String errorResponse = "HTTP/1.1 500 Internal Server Error\r\n" +
                                 "Content-Type: application/json\r\n" +
                                 "\r\n" +
                                 "{\"success\":false,\"error\":\"SERVER_ERROR\",\"message\":\"Internal server error\"}";
            out.write(errorResponse.getBytes());
        }
    }
    
    private String processRequest(String method, String path, String requestBody, Map<String, String> headers) {
        try {
            // CORS preflight
            if ("OPTIONS".equals(method)) {
                return "{\"success\":true}";
            }
            
            // Parse path and parameters
            String[] pathParts = path.split("\\?");
            String endpoint = pathParts[0];
            Map<String, String> queryParams = parseQueryParams(pathParts.length > 1 ? pathParts[1] : "");
            
            // Routing
            switch (endpoint) {
                case "/employees":
                    return handleEmployeesEndpoint(method, queryParams, requestBody);
                    
                case "/database/create":
                    if ("POST".equals(method)) return databaseController.createDatabase(requestBody);
                    break;
                    
                case "/database/load":
                    if ("POST".equals(method)) return databaseController.loadDatabase(requestBody);
                    break;
                    
                case "/database/info":
                    if ("GET".equals(method)) return databaseController.getDatabaseInfo();
                    break;
                    
                case "/database/backup":
                    if ("POST".equals(method)) return databaseController.backupDatabase();
                    break;
                    
                case "/database/clear":
                    if ("DELETE".equals(method)) return databaseController.clearDatabase();
                    break;
                    
                default:
                    // Check for /employees/{id}
                    if (endpoint.startsWith("/employees/")) {
                        String idParam = endpoint.substring("/employees/".length());
                        return handleEmployeeByIdEndpoint(method, idParam, requestBody);
                    }
            }
            
            // Endpoint not found
            return "{\"success\":false,\"error\":\"ENDPOINT_NOT_FOUND\",\"message\":\"Endpoint not found: " + endpoint + "\"}";
            
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"REQUEST_PROCESSING_ERROR\",\"message\":\"Error processing request: " + e.getMessage() + "\"}";
        }
    }
    
    private String handleEmployeesEndpoint(String method, Map<String, String> queryParams, String requestBody) {
        switch (method) {
            case "GET":
                if (!queryParams.isEmpty()) {
                    return employeeController.searchEmployees(queryParams);
                } else {
                    return employeeController.getAllEmployees();
                }
                
            case "POST":
                return employeeController.createEmployee(requestBody);
                
            case "DELETE":
                return employeeController.deleteEmployeesByCriteria(queryParams);
                
            default:
                return "{\"success\":false,\"error\":\"METHOD_NOT_ALLOWED\",\"message\":\"Method not allowed for /employees\"}";
        }
    }
    
    private String handleEmployeeByIdEndpoint(String method, String idParam, String requestBody) {
        switch (method) {
            case "GET":
                return employeeController.getEmployeeById(idParam);
                
            case "PUT":
                return employeeController.updateEmployee(idParam, requestBody);
                
            case "DELETE":
                return employeeController.deleteEmployee(idParam);
                
            default:
                return "{\"success\":false,\"error\":\"METHOD_NOT_ALLOWED\",\"message\":\"Method not allowed for /employees/{id}\"}";
        }
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
