import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors; // Import this
import org.json.simple.*;
import org.json.simple.parser.*;

public class Main {
    // Use a thread-safe map
    private static Map<String, Integer> results = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Serve static files (HTML, CSS, JS)
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";

            File file = new File("." + path);
            if (file.exists() && !file.isDirectory()) {
                byte[] bytes = Files.readAllBytes(file.toPath());
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } else {
                String msg = "404 Not Found";
                exchange.sendResponseHeaders(404, msg.length());
                exchange.getResponseBody().write(msg.getBytes());
                exchange.close();
            }
        });

        // API to load questions
        server.createContext("/api/questions", exchange -> {
            String json = Files.readString(Path.of("questions.json"));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.length());
            exchange.getResponseBody().write(json.getBytes());
            exchange.close();
        });

        // API to submit results
        server.createContext("/api/submit", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                String data = reader.readLine();
                Map<String, String> params = new HashMap<>();
                for (String pair : data.split("&")) {
                    String[] kv = pair.split("=");
                    if (kv.length == 2) params.put(kv[0], kv[1]);
                }
                String name = params.get("student");
                int score = Integer.parseInt(params.get("score"));
                results.put(name, score); // Store the result
                String response = "Result saved for " + name + " (Score: " + score + ")";
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
            }
        });

        // --- NEW ENDPOINT TO VIEW RESULTS ---
        server.createContext("/api/results", exchange -> {
            // Convert the Map<String, Integer> to a JSON string
            // e.g., {"Shreyash": 8, "Gautam": 7}
            String jsonResponse = results.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\": " + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jsonResponse.length());
            exchange.getResponseBody().write(jsonResponse.getBytes());
            exchange.close();
        });
        // --- END OF NEW ENDPOINT ---

        server.start();
        System.out.println("Server started at http://localhost:8080");
    }
}