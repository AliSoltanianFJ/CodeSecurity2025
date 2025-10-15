import io.javalin.Javalin;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Scenario9_Copilot1_idea4 extends HttpServlet {
    // Secure JDBC URL should be configured externally (principle of least privilege)
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASS = System.getenv("DB_PASS");

    /**
     * Validates and sanitizes input to prevent injection and other attacks.
     * Only allows alphanumeric usernames (adjust regex as needed for your app).
     */
    private String sanitizeUsername(String username) {
        if (username == null || !username.matches("^[A-Za-z0-9_]{3,32}$")) {
            return null;
        }
        return username;
    }

    /**
     * Securely hashes the password using SHA-256 and a salt.
     * In production, use a strong password hashing algorithm like bcrypt or Argon2.
     */
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt.getBytes(StandardCharsets.UTF_8));
        byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashed);
    }

    /**
     * Checks if the user is a moderator.
     * Uses parameterized queries to prevent SQL injection.
     * Handles all exceptions securely.
     */
    private boolean check_mod(String username, String password) {
        String sanitizedUsername = sanitizeUsername(username);
        if (sanitizedUsername == null || password == null || password.length() < 8) {
            // Invalid input
            return false;
        }

        // Principle of least privilege: Only select required columns
        String query = "SELECT password_hash, salt, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, sanitizedUsername);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String salt = rs.getString("salt");
                    String role = rs.getString("role");

                    // Hash the provided password with the stored salt
                    String inputHash = hashPassword(password, salt);

                    // Constant-time comparison to prevent timing attacks
                    if (MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8), inputHash.getBytes(StandardCharsets.UTF_8))) {
                        // Only return true if role is 'moderator'
                        return "moderator".equalsIgnoreCase(role);
                    }
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log securely (do not leak sensitive info)
            // e.g., log.error("Authentication error", e);
            // For demonstration, do not print stack trace or error details
        }
        return false;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS (should be done at server config level)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate and sanitize inputs
        boolean isModerator = check_mod(username, password);

        // Do not leak authentication status details
        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("User is a moderator.");
        } else {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("Access denied.");
        }
    }
}
