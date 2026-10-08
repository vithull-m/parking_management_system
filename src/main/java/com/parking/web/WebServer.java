package com.parking.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.parking.dao.*;
import com.parking.db.DatabaseManager;
import com.parking.exception.UnauthorizedException;
import com.parking.exception.ValidationException;
import com.parking.model.*;
import com.parking.service.AuthService;
import com.parking.service.AvailabilityService;
import com.parking.service.BookingService;
import com.parking.service.ResourceManagementService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;

public class WebServer {

    private static final int PORT = 8080;
    private final DatabaseManager dbManager;
    private final UserDao userDao;
    private final ScheduleDao scheduleDao;
    private final ResourceDao resourceDao;
    private final ReservationDao reservationDao;
    private final BookingStatusHistoryDao historyDao;

    private final AuthService authService;
    private final AvailabilityService availabilityService;
    private final BookingService bookingService;
    private final ResourceManagementService resourceManagementService;

    private final Gson gson;
    private HttpServer server;

    public WebServer(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.userDao = new UserDao(dbManager);
        this.scheduleDao = new ScheduleDao(dbManager);
        this.resourceDao = new ResourceDao(dbManager);
        this.historyDao = new BookingStatusHistoryDao(dbManager);
        this.reservationDao = new ReservationDao(dbManager, historyDao);

        this.authService = new AuthService(userDao);
        this.availabilityService = new AvailabilityService(resourceDao);
        this.bookingService = new BookingService(dbManager, resourceDao, reservationDao, historyDao, userDao);
        this.resourceManagementService = new ResourceManagementService(resourceDao, scheduleDao, userDao);

        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
                .create();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API Contexts
        server.createContext("/api/auth/login", this::handleLogin);
        server.createContext("/api/auth/register", this::handleRegister);
        server.createContext("/api/auth/users", this::handleGetUsers);

        server.createContext("/api/slots", this::handleSlots);
        server.createContext("/api/bookings/calculate", this::handleCalculateFee);
        server.createContext("/api/bookings", this::handleBookings);

        server.createContext("/api/admin/bookings", this::handleAdminBookings);
        server.createContext("/api/admin/status", this::handleAdminStatusUpdate);
        server.createContext("/api/admin/stats", this::handleAdminStats);
        server.createContext("/api/admin/slots", this::handleAdminSlots);

        // Static Files Handler
        server.createContext("/", this::handleStaticFiles);

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("===============================================================");
        System.out.println("🚀 Vehicle Parking Management Web Application is LIVE!");
        System.out.println("🌐 Access Web App at: http://localhost:" + PORT);
        System.out.println("===============================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // Handlers
    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
            return;
        }
        try {
            String body = readRequestBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String username = json.has("username") ? json.get("username").getAsString() : "";
            String password = json.has("password") ? json.get("password").getAsString() : "";

            User user = authService.login(username, password);
            sendResponse(exchange, 200, Map.of(
                    "success", true,
                    "user", user
            ));
        } catch (ValidationException ve) {
            sendResponse(exchange, 400, Map.of("error", ve.getMessage()));
        } catch (Exception e) {
            sendResponse(exchange, 500, Map.of("error", "Login failed: " + e.getMessage()));
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
            return;
        }
        try {
            String body = readRequestBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String username = json.has("username") ? json.get("username").getAsString() : "";
            String password = json.has("password") ? json.get("password").getAsString() : "";
            String fullName = json.has("fullName") ? json.get("fullName").getAsString() : "";
            String phone = json.has("phone") ? json.get("phone").getAsString() : "";

            User newUser = authService.register(username, password, fullName, phone);
            sendResponse(exchange, 201, Map.of(
                    "success", true,
                    "message", "User registered successfully!",
                    "user", newUser
            ));
        } catch (ValidationException ve) {
            sendResponse(exchange, 400, Map.of("error", ve.getMessage()));
        } catch (Exception e) {
            sendResponse(exchange, 500, Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }

    private void handleGetUsers(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
            return;
        }
        List<User> users = authService.getAllUsers();
        sendResponse(exchange, 200, Map.of("users", users));
    }

    private void handleSlots(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
            return;
        }
        Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
        String q = queryParams.get("q");
        List<Resource> slots = availabilityService.searchActiveSlots(q);
        sendResponse(exchange, 200, Map.of("slots", slots));
    }

    private void handleCalculateFee(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
            return;
        }
        try {
            String body = readRequestBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            double rate = json.get("rate").getAsDouble();
            int quantity = json.get("quantity").getAsInt();

            FeeBreakdown fee = bookingService.calculateFee(rate, quantity);
            sendResponse(exchange, 200, fee);
        } catch (ValidationException ve) {
            sendResponse(exchange, 400, Map.of("error", ve.getMessage()));
        } catch (Exception e) {
            sendResponse(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleBookings(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("POST".equalsIgnoreCase(method)) {
            // Check if it's a cancellation: /api/bookings/{id}/cancel
            if (path.endsWith("/cancel")) {
                handleCancelBooking(exchange);
                return;
            }

            // Create new booking
            try {
                String body = readRequestBody(exchange);
                BookingRequest req = gson.fromJson(body, BookingRequest.class);
                Reservation res = bookingService.confirmBooking(req);
                sendResponse(exchange, 201, Map.of(
                        "success", true,
                        "message", "Booking confirmed successfully!",
                        "reservation", res
                ));
            } catch (ValidationException ve) {
                sendResponse(exchange, 400, Map.of("error", ve.getMessage()));
            } catch (Exception e) {
                sendResponse(exchange, 500, Map.of("error", "Booking failed: " + e.getMessage()));
            }
        } else if ("GET".equalsIgnoreCase(method)) {
            // Check if single booking or list
            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
            String userIdStr = queryParams.get("userId");
            if (userIdStr == null) {
                sendResponse(exchange, 400, Map.of("error", "userId parameter is required."));
                return;
            }
            Long userId = Long.parseLong(userIdStr);

            // Single booking detail check or history
            String[] segments = path.split("/");
            if (segments.length >= 4 && !segments[3].isEmpty()) {
                Long bookingId = Long.parseLong(segments[3]);
                if (segments.length >= 5 && "history".equalsIgnoreCase(segments[4])) {
                    List<BookingStatusHistory> history = bookingService.getStatusHistory(bookingId, userId);
                    sendResponse(exchange, 200, Map.of("history", history));
                    return;
                }
                Reservation res = bookingService.getReservationById(bookingId, userId);
                sendResponse(exchange, 200, Map.of("reservation", res));
                return;
            }

            // List bookings for user
            List<Reservation> userBookings = bookingService.getUserBookings(userId, userId);
            sendResponse(exchange, 200, Map.of("bookings", userBookings));
        } else {
            sendResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
        }
    }

    private void handleCancelBooking(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath(); // /api/bookings/{id}/cancel
            String[] parts = path.split("/");
            Long bookingId = Long.parseLong(parts[3]);

            String body = readRequestBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            Long userId = json.get("userId").getAsLong();

            Reservation res = bookingService.cancelBooking(bookingId, userId);
            sendResponse(exchange, 200, Map.of(
                    "success", true,
                    "message", "Booking cancelled successfully. Slot availability restored.",
                    "reservation", res
            ));
        } catch (ValidationException | UnauthorizedException ve) {
            sendResponse(exchange, 400, Map.of("error", ve.getMessage()));
        } catch (Exception e) {
            sendResponse(exchange, 500, Map.of("error", "Cancellation failed: " + e.getMessage()));
        }
    }

    private void handleAdminBookings(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
            return;
        }
        try {
            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
            String adminIdStr = queryParams.get("userId");
            if (adminIdStr == null) {
                sendResponse(exchange, 401, Map.of("error", "userId parameter is required for Admin authorization."));
                return;
            }
            Long adminId = Long.parseLong(adminIdStr);
            List<Reservation> allBookings = bookingService.getAllBookings(adminId);
            sendResponse(exchange, 200, Map.of("bookings", allBookings));
        } catch (UnauthorizedException ue) {
            sendResponse(exchange, 403, Map.of("error", ue.getMessage()));
        } catch (Exception e) {
            sendResponse(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleAdminStatusUpdate(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
            return;
        }
        try {
            String body = readRequestBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            Long bookingId = json.get("bookingId").getAsLong();
            Long adminId = json.get("adminUserId").getAsLong();
            String targetStatusStr = json.get("targetStatus").getAsString();
            BookingStatus targetStatus = BookingStatus.valueOf(targetStatusStr);

            Reservation updated = bookingService.updateBookingStatusByAdmin(bookingId, adminId, targetStatus);
            sendResponse(exchange, 200, Map.of(
                    "success", true,
                    "message", "Status updated to " + targetStatus.getDisplayName(),
                    "reservation", updated
            ));
        } catch (ValidationException ve) {
            sendResponse(exchange, 400, Map.of("error", ve.getMessage()));
        } catch (UnauthorizedException ue) {
            sendResponse(exchange, 403, Map.of("error", ue.getMessage()));
        } catch (Exception e) {
            sendResponse(exchange, 500, Map.of("error", "Status update failed: " + e.getMessage()));
        }
    }

    private void handleAdminStats(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
            return;
        }
        try {
            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
            String adminIdStr = queryParams.get("userId");
            if (adminIdStr == null) {
                sendResponse(exchange, 401, Map.of("error", "userId required."));
                return;
            }
            Long adminId = Long.parseLong(adminIdStr);
            List<Reservation> all = bookingService.getAllBookings(adminId);
            List<Resource> slots = resourceDao.findAll();

            long total = all.size();
            long confirmed = all.stream().filter(r -> r.getStatus() == BookingStatus.CONFIRMED).count();
            long checkedIn = all.stream().filter(r -> r.getStatus() == BookingStatus.CHECKED_IN).count();
            long completed = all.stream().filter(r -> r.getStatus() == BookingStatus.COMPLETED).count();
            long cancelled = all.stream().filter(r -> r.getStatus() == BookingStatus.CANCELLED).count();
            double revenue = all.stream()
                    .filter(r -> r.getStatus() != BookingStatus.CANCELLED)
                    .mapToDouble(Reservation::getGrandTotal)
                    .sum();

            sendResponse(exchange, 200, Map.of(
                    "metrics", Map.of(
                            "total", total,
                            "confirmed", confirmed,
                            "checkedIn", checkedIn,
                            "completed", completed,
                            "cancelled", cancelled,
                            "revenue", revenue
                    ),
                    "slots", slots,
                    "schedules", scheduleDao.findAll()
            ));
        } catch (UnauthorizedException ue) {
            sendResponse(exchange, 403, Map.of("error", ue.getMessage()));
        } catch (Exception e) {
            sendResponse(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleAdminSlots(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method)) {
            sendResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(readRequestBody(exchange)).getAsJsonObject();
            if (!json.has("adminUserId") || json.get("adminUserId").isJsonNull()) {
                throw new ValidationException("Administrator ID is required.");
            }
            Long adminUserId = json.get("adminUserId").getAsLong();
            Resource resource = new Resource();
            resource.setIdentifier(requiredString(json, "identifier"));
            resource.setName(requiredString(json, "name"));
            resource.setScheduleId(requiredLong(json, "scheduleId"));
            resource.setRate(requiredDouble(json, "rate"));
            resource.setTotalCapacity(requiredInt(json, "totalCapacity"));
            if (!json.has("active") || json.get("active").isJsonNull()) {
                throw new ValidationException("Spot status is required.");
            }
            resource.setActive(json.get("active").getAsBoolean());

            Resource saved;
            if ("POST".equalsIgnoreCase(method)) {
                saved = resourceManagementService.addSpot(adminUserId, resource);
                sendResponse(exchange, 201, Map.of("success", true, "message", "Parking spot added.", "spot", saved));
            } else {
                Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
                String id = queryParams.get("id");
                if (id == null) {
                    throw new ValidationException("Spot ID is required for updates.");
                }
                saved = resourceManagementService.updateSpot(adminUserId, Long.parseLong(id), resource);
                sendResponse(exchange, 200, Map.of("success", true, "message", "Parking spot updated.", "spot", saved));
            }
        } catch (ValidationException ve) {
            sendResponse(exchange, 400, Map.of("error", ve.getMessage()));
        } catch (UnauthorizedException ue) {
            sendResponse(exchange, 403, Map.of("error", ue.getMessage()));
        } catch (IllegalStateException | NumberFormatException | JsonParseException e) {
            sendResponse(exchange, 400, Map.of("error", "Invalid parking spot details."));
        } catch (Exception e) {
            sendResponse(exchange, 500, Map.of("error", "Parking spot could not be saved: " + e.getMessage()));
        }
    }

    private String requiredString(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            throw new ValidationException(key + " is required.");
        }
        return json.get(key).getAsString();
    }

    private Long requiredLong(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            throw new ValidationException(key + " is required.");
        }
        return json.get(key).getAsLong();
    }

    private double requiredDouble(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            throw new ValidationException(key + " is required.");
        }
        return json.get(key).getAsDouble();
    }

    private int requiredInt(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            throw new ValidationException(key + " is required.");
        }
        return json.get(key).getAsInt();
    }

    private void handleStaticFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/".equals(path) || path.isEmpty()) {
            path = "/index.html";
        }

        // Search in src/main/resources/static or target classes
        Path filePath = Paths.get("src/main/resources/static" + path);
        if (!Files.exists(filePath)) {
            filePath = Paths.get("target/classes/static" + path);
        }

        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            String contentType = getContentType(path);
            byte[] bytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            // Serve index.html for SPA fallback or 404
            Path indexPath = Paths.get("src/main/resources/static/index.html");
            if (Files.exists(indexPath)) {
                byte[] bytes = Files.readAllBytes(indexPath);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                String notFound = "<h1>404 Not Found</h1>";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
            }
        }
    }

    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".json")) return "application/json; charset=UTF-8";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "text/plain";
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    public static void main(String[] args) {
        try {
            DatabaseManager db = new DatabaseManager();
            db.initializeDatabase();

            WebServer server = new WebServer(db);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
