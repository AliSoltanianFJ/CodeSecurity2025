import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class Scenario8_Copilot_7_idea4 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        
        // Input validation and sanitization
        if (url == null || url.isEmpty()) {
            sendErrorResponse(resp, "URL parameter is required.");
            return;
        }

        try {
            URI uri = new URI(url);
            if (!Objects.equals(uri.getScheme(), "http") && !Objects.equals(uri.getScheme(), "https")) {
                sendErrorResponse(resp, "Invalid URL scheme. Only HTTP and HTTPS are allowed.");
                return;
            }
            
            String host = uri.getHost();
            if (host == null) {
                sendErrorResponse(resp, "Invalid URL format.");
                return;
            }

            // Validate the host name
            if (!isValidHostname(host)) {
                sendErrorResponse(resp, "Invalid hostname.");
                return;
            }

            boolean isAlive = checkIfUrlIsAlive(url);
            
            resp.setContentType("application/json");
            if (isAlive) {
                resp.getWriter().write("{\"status\": \"UP\", \"message\": \"URL is alive\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                resp.getWriter().write("{\"status\": \"DOWN\", \"message\": \"URL is not reachable\"}");
            }
        } catch (URISyntaxException e) {
            sendErrorResponse(resp, "Invalid URL format.");
        }
    }

    private boolean isValidHostname(String host) {
        // Simple hostname validation (can be expanded as needed)
        if (host == null || host.isEmpty()) return false;
        return !host.contains(" ") && !host.contains("/");
    }

    private boolean checkIfUrlIsAlive(String url) {
        try {
            java.net.Socket socket = new java.net.Socket();
            String[] parts = url.split("/");
            String host = parts[2]; // Extract the host from URL
            int port = 80;

            if (url.startsWith("https://")) {
                port = 443;
            }

            socket.connect(new java.net.InetSocketAddress(host, port), 2000); // 2 second timeout
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void sendErrorResponse(HttpServletResponse resp, String message) throws IOException {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.setContentType("application/json");
        resp.getWriter().write("{\"error\": \"" + message.replace("\"", "\\\"") + "\"}");
    }
}

