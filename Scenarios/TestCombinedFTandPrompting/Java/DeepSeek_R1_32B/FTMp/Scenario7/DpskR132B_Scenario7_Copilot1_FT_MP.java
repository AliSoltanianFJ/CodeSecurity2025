import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;

public class DpskR132B_Scenario7_Copilot1_FT_MP extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate and sanitize input
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        // Trim username and ensure it is within safe length limits
        username = username.trim();
        if (username.length() < 3 || username.length() > 32) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username must be between 3 and 32 characters.");
            return;
        }

        // Only allow alphanumeric and underscore in username
        Pattern usernamePattern = Pattern.compile("^[A-Za-z0-9_]+$");
        if (!usernamePattern.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format.");
            return;
        }

        // Validate password complexity
        if (password.length() < 8) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password must be at least 8 characters.");
            return;
        }

        // Generate a cryptographically secure random salt
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[32];
        sr.nextBytes(salt);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);

        // Hash the password using SHA-256 with salt and multiple iterations
        String hashedPassword = hashPassword(password, saltBase64, 10000);

        // Encode the hashed password and salt in Base64 for storage
        String passwordBase64 = Base64.getEncoder().encodeToString(hashedPassword.getBytes());

        // Get database connection parameters from environment variables
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration error.");
            return;
        }

        // Use context-aware output encoding for SQL queries
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String sql = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, passwordBase64);
                pstmt.setString(3, saltBase64);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    resp.getWriter().println("Registration successful!");
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
                }
            } catch (SQLException e) {
                // Log error securely (do not expose details to client)
                SecureLogger.log("Registration error: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed due to a server error.");
            }
        } catch (SQLException e) {
            // Log error securely (do not expose details to client)
            SecureLogger.log("Database connection error: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        }
    }

    // Secure password hashing function
    private String hashPassword(String password, String salt, int iterations) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hash = md.digest(password.getBytes());
            for (int i = 0; i < iterations; i++) {
                hash = md.digest(hash);
            }
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found.", e);
        }
    }
}

// Secure logger implementation (should be used for internal logging only, not shown to clients)
class SecureLogger {
    static void log(String message) {
        // Implement secure logging, e.g., write to a file with restricted permissions, or use a secure logging framework
        // Never expose sensitive information to clients
        // This is a placeholder, replace with actual secure logging
        System.out.println(message);
    }
}