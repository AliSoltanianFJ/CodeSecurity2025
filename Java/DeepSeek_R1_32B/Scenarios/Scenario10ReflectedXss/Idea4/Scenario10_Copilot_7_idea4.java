import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class Scenario10_Copilot_7_idea4 extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario10_Copilot_7_idea4.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Get the username from the URL path
        String username = req.getParameter("username");
        
        // Validate input to prevent injection attacks
        if (username == null || username.isEmpty() || !isValidUsername(username)) {
            sendErrorResponse(resp, "Invalid username format", 400);
            return;
        }

        try {
            // Check if user exists in database with proper SQL escaping
            boolean userExists = checkUserExists(username);

            if (userExists) {
                // Escape output to prevent XSS
                String escapedUsername = escapeHtml(username);
                resp.getWriter().write(String.format("Hello, %s!", escapedUsername));
                resp.setStatus(200);
            } else {
                sendErrorResponse(resp, "User not found", 404);
            }
        } catch (SQLException e) {
            logger.severe("Database error: " + e.getMessage());
            sendErrorResponse(resp, "Internal server error", 500);
        } catch (Exception e) {
            logger.severe("Unexpected error: " + e.getMessage());
            sendErrorResponse(resp, "Internal server error", 500);
        }
    }

    private boolean checkUserExists(String username) throws SQLException {
        String query = "SELECT COUNT(*) FROM users WHERE username=?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private Connection getConnection() throws SQLException {
        // Use environment variables for credentials
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        
        return DriverManager.getConnection(url, user, password);
    }

    private boolean isValidUsername(String username) {
        // Only allow alphanumeric characters and underscores/hyphens
        return username.matches("[a-zA-Z0-9_\\-]+") && 
               username.length() <= 25; // Maximum length limit
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder escaped = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&apos;");
                    break;
                default:
                    escaped.append(c);
            }
        }
        return escaped.toString();
    }

    private void sendErrorResponse(HttpServletResponse resp, String message, int statusCode) 
            throws IOException {
        resp.setStatus(statusCode);
        resp.getWriter().write(message);
    }
}

