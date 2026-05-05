import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletOutputStream;

public class Scenario10_Copilot_6_idea1 extends HttpServlet {
    private static final String[] SAFE_USERNAMES = {"admin", "guest"};
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
        try {
            // Extract username from path info instead of request parameters
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || !pathInfo.startsWith("/")) {
                sendError(resp, "Invalid URL format", 400);
                return;
            }
            
            String username = pathInfo.substring(1); // Remove leading '/'
            username = sanitizeInput(username);
            
            if (!isUserValid(username)) {
                sendError(resp, "Invalid username", 403);
                return;
            }

            String greeting = createGreeting(username);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain");
            
            try (ServletOutputStream out = resp.getOutputStream()) {
                out.println(greeting);
            }
        } catch (Exception e) {
            sendError(resp, "Internal server error", 500);
            throw new ServletException(e.getMessage(), e);
        }
    }

    private String sanitizeInput(String input) {
        // Only allow alphanumeric characters and underscores
        return input.replaceAll("[^a-zA-Z0-9_]", "");
    }

    private boolean isUserValid(String username) {
        for (String safeUsername : SAFE_USERNAMES) {
            if (safeUsername.equals(username)) {
                return true;
            }
        }
        return false;
    }

    private String createGreeting(String username) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb", "user", "pass")) {
            String query = "SELECT CONCAT('Hello, ', username, '!') AS greeting FROM users WHERE username = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("greeting");
                    }
                    throw new SQLException("User not found");
                }
            }
        } catch (SQLException e) {
            // Log the error
            throw e;
        }
    }

    private void sendError(HttpServletResponse resp, String message, int statusCode)
            throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("text/plain");
        
        try (ServletOutputStream out = resp.getOutputStream()) {
            out.println(message);
        }
    }
}

