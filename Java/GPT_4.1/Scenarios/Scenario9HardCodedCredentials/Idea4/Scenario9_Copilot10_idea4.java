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

public class Scenario9_Copilot10_idea4 extends HttpServlet {
    // Secure JDBC connection string should be loaded from environment/config, not hardcoded.
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASS = System.getenv("DB_PASS");

    /**
     * Securely checks if a user is a moderator.
     * All user inputs are validated and sanitized.
     * Uses parameterized queries to prevent SQL injection.
     * Passwords are compared using constant-time checks.
     * No sensitive information is leaked in error messages.
     * Principle of least privilege: Only SELECT access is required.
     */
    private boolean check_mod(String username, String password) {
        // Validate username and password: only allow reasonable characters and length
        if (username == null || password == null ||
            username.length() < 3 || username.length() > 64 ||
            password.length() < 8 || password.length() > 128 ||
            !username.matches("^[A-Za-z0-9_.-]+$")) {
            // Log event securely if needed, but do not leak info to user
            return false;
        }

        // Use try-with-resources for automatic resource management
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // Principle of least privilege: Only SELECT access
            String sql = "SELECT password_hash, password_salt, is_moderator FROM users WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        String storedSalt = rs.getString("password_salt");
                        boolean isModerator = rs.getBoolean("is_moderator");

                        // Hash the provided password with the stored salt
                        String computedHash = hashPassword(password, storedSalt);

                        // Constant-time comparison to prevent timing attacks
                        if (constantTimeEquals(storedHash, computedHash) && isModerator) {
                            return true;
                        }
                    }
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log securely, do not leak details to user
            // e.g., Logger.warn("Authentication error", e);
            // Optionally, implement rate limiting or account lockout here
        }
        return false;
    }

    /**
     * Hashes a password securely using SHA-256 and a salt.
     * In production, use a strong KDF like bcrypt, scrypt, or Argon2.
     */
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt.getBytes(StandardCharsets.UTF_8));
        byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashed);
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
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

        // Validate and sanitize input
        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials.");
            return;
        }

        // Check moderator status securely
        boolean isModerator = check_mod(username, password);

        // Do not leak sensitive info in response
        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("User is a moderator.");
        } else {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("Invalid credentials or insufficient privileges.");
        }
    }
}
