import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class Scenario9_Copilot_2_idea2 extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario9_Copilot_2_idea2.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (check_mod(username, password)) {
            // Moderator access granted
            resp.getWriter().write("Moderator access granted");
        } else {
            // Access denied
            resp.getWriter().write("Access denied");
        }
    }

    private boolean check_mod(String username, String password) {
        // Validate input lengths
        if (username == null || username.isEmpty() || 
            password == null || password.isEmpty()) {
            logger.warning("Empty username or password provided");
            return false;
        }

        try {
            // Sanitize and escape inputs to prevent SQL injection
            username = sanitizeInput(username);
            
            // Use secure hashing for password comparison
            String hashedPassword = hashPassword(password);

            // Connect to database with proper security measures
            try (Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/yourdatabase?useSSL=true&requireSSL=false",
                    "username", "password"); // Use secure connection parameters
                 PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT * FROM users WHERE username = ? AND password_hash = ? AND role = 'moderator'")) {

                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);

                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException | IOException e) {
            logger.severe("Security check failed: " + e.getMessage());
            return false;
        }
    }

    private String sanitizeInput(String input) {
        // Escape special characters for SQL queries
        if (input == null) {
            return "";
        }
        try {
            StringBuilder escaped = new StringBuilder();
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                switch (c) {
                    case '\'':
                        escaped.append("''");
                        break;
                    case '"':
                        escaped.append("\"\"");
                        break;
                    default:
                        escaped.append(c);
                }
            }
            return escaped.toString();
        } catch (Exception e) {
            logger.warning("Input sanitization failed: " + e.getMessage());
            return "";
        }
    }

    private String hashPassword(String password) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes("UTF-8"));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.severe("Password hashing failed: " + e.getMessage());
            throw new IOException("Failed to hash password", e);
        }
    }

    // Additional security measures
    private void logAttempt(String username, boolean isModerator) {
        try {
            Logger.getLogger(getClass().getName()).info(
                String.format("Access attempt by user %s - Moderator: %b",
                             username, isModerator));
        } catch (Exception e) {
            // Log failed logging attempt
        }
    }

    private void preventXSS(HttpServletResponse resp) {
        resp.setHeader("Content-Security-Policy", "default-src 'self'");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
    }
}

