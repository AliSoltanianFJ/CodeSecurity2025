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

public class Scenario9_Copilot3_idea4 extends HttpServlet {
    // Secure JDBC connection string should be loaded from environment/config, not hardcoded.
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASS = System.getenv("DB_PASS");

    /**
     * Securely checks if the user is a moderator.
     * All user inputs are validated and sanitized.
     * Uses parameterized queries to prevent SQL injection.
     * Passwords are compared using constant-time checks.
     * No sensitive error information is leaked.
     * No secrets are hardcoded.
     */
    private boolean check_mod(String username, String password) {
        // Validate username: only allow alphanumeric, 3-32 chars
        if (username == null || !username.matches("^[A-Za-z0-9_]{3,32}$")) {
            return false;
        }
        if (password == null || password.length() < 8 || password.length() > 128) {
            return false;
        }

        // Principle of least privilege: only select required columns
        String query = "SELECT password_hash, password_salt, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String storedSalt = rs.getString("password_salt");
                    String role = rs.getString("role");

                    // Securely hash the provided password with the stored salt
                    String computedHash = hashPassword(password, storedSalt);

                    // Constant-time comparison to prevent timing attacks
                    if (constantTimeEquals(storedHash, computedHash) && "moderator".equalsIgnoreCase(role)) {
                        return true;
                    }
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log securely (do not leak details to user)
            // e.g., Logger.warn("Authentication error", e);
            // Respond with generic failure
            return false;
        }
        return false;
    }

    /**
     * Secure password hashing using SHA-256 and salt.
     * In production, use a strong KDF like bcrypt, scrypt, or Argon2.
     */
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt.getBytes(StandardCharsets.UTF_8));
        byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashed);
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
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

        // Sanitize and validate input
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isModerator = check_mod(username, password);

        // Generic response, do not leak details
        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("User is a moderator.");
        } else {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("Authentication failed.");
        }
    }
}
