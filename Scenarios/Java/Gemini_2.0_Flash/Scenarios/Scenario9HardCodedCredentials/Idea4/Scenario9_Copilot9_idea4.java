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

public class Scenario9_Copilot9_idea4 extends HttpServlet {
    // Secure JDBC connection details should be stored in environment variables or a secure vault, never hardcoded.
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASS = System.getenv("DB_PASS");

    /**
     * Validates and sanitizes input to prevent injection and other attacks.
     * @param input The user input string.
     * @return Sanitized string or null if invalid.
     */
    private String sanitizeInput(String input) {
        if (input == null) return null;
        // Only allow alphanumeric usernames, length 3-32
        if (!input.matches("^[A-Za-z0-9_]{3,32}$")) return null;
        return input;
    }

    /**
     * Securely checks if the user is a moderator.
     * @param username The sanitized username.
     * @param password The raw password.
     * @return true if moderator, false otherwise.
     */
    private boolean check_mod(String username, String password) {
        // Principle of least privilege: restrict query to only necessary columns
        String query = "SELECT password_hash, password_salt, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String storedSalt = rs.getString("password_salt");
                    String role = rs.getString("role");

                    // Secure password verification using SHA-256 and salt
                    String computedHash = hashPassword(password, storedSalt);
                    if (storedHash.equals(computedHash) && "moderator".equalsIgnoreCase(role)) {
                        return true;
                    }
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log securely (never expose details to user)
            // e.g., Logger.warn("Authentication error", e);
        }
        return false;
    }

    /**
     * Hashes password securely using SHA-256 and salt.
     * @param password The raw password.
     * @param salt The salt from DB.
     * @return Base64-encoded hash.
     * @throws NoSuchAlgorithmException
     */
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt.getBytes(StandardCharsets.UTF_8));
        byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashed);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS (should be configured at server level)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        String username = sanitizeInput(req.getParameter("username"));
        String password = req.getParameter("password"); // Passwords may contain any character

        if (username == null || password == null || password.length() < 8 || password.length() > 64) {
            // Do not reveal which field failed
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials.");
            return;
        }

        boolean isModerator = check_mod(username, password);

        // Generic error message to avoid information leakage
        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Moderator access granted.");
        } else {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access denied.");
        }
    }
}
