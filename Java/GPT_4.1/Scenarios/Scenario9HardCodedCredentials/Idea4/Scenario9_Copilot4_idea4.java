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
import java.util.Base64;
import java.util.regex.Pattern;

public class Scenario9_Copilot4_idea4 extends HttpServlet {
    // Secure regex for username validation (alphanumeric, 3-32 chars)
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,32}$");
    // Secure password length requirement
    private static final int MIN_PASSWORD_LENGTH = 8;
    // Database connection string should be securely managed (not hardcoded)
    private static final String DB_URL = System.getenv("SCENARIO9_DB_URL");
    private static final String DB_USER = System.getenv("SCENARIO9_DB_USER");
    private static final String DB_PASS = System.getenv("SCENARIO9_DB_PASS");

    /**
     * Securely checks if the user is a moderator.
     * All user inputs are validated and sanitized.
     * Uses parameterized queries to prevent SQL injection.
     * Passwords are compared using constant-time checks.
     * No sensitive information is leaked in error messages.
     * All secrets are loaded from environment variables.
     */
    private boolean check_mod(String username, String password) {
        // Validate username format
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            return false;
        }
        // Validate password length
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }

        // Principle of least privilege: Only query required columns
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
            // Log error securely (do not leak details to client)
            // e.g., Logger.warn("Authentication error", e);
            // For demonstration, do not print stack trace or error details
        }
        return false;
    }

    /**
     * Secure password hashing using SHA-256 and salt.
     * In production, use a stronger algorithm like bcrypt or Argon2.
     */
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(Base64.getDecoder().decode(salt));
        byte[] hashed = md.digest(password.getBytes());
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

        // Enforce HTTPS (should be handled at server config level)
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

        boolean isModerator = check_mod(username, password);

        // Do not leak authentication details
        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("User is a moderator.");
        } else {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("Authentication failed.");
        }
    }
}
