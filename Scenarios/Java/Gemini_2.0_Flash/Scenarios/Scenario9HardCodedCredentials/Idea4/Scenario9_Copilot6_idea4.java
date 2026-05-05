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

public class Scenario9_Copilot6_idea4 extends HttpServlet {
    // Secure database connection details should be stored in environment variables or a secure vault.
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    /**
     * Securely checks if the user is a moderator.
     * All user inputs are validated and sanitized.
     * Uses parameterized queries to prevent SQL injection.
     * Passwords are assumed to be stored as salted hashes.
     * No sensitive information is leaked in error messages.
     * Principle of least privilege: only SELECT privilege required.
     */
    private boolean check_mod(String username, String password) {
        // Validate username: only allow alphanumeric, 3-32 chars
        if (username == null || !username.matches("^[a-zA-Z0-9]{3,32}$")) {
            return false;
        }
        if (password == null || password.length() < 8 || password.length() > 64) {
            return false;
        }

        // Securely connect to the database
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Principle of least privilege: only select required columns
            String sql = "SELECT password_hash, password_salt, role FROM users WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        String storedSalt = rs.getString("password_salt");
                        String role = rs.getString("role");

                        // Hash the provided password with the stored salt
                        String computedHash = hashPassword(password, storedSalt);

                        // Constant-time comparison to prevent timing attacks
                        if (MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8),
                                computedHash.getBytes(StandardCharsets.UTF_8))) {
                            // Check if user is a moderator
                            return "moderator".equalsIgnoreCase(role);
                        }
                    }
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log securely (do not leak details to user)
            // e.g., Logger.warn("Authentication error", e);
            // For this example, we do not expose any details.
        }
        return false;
    }

    /**
     * Securely hashes a password with a salt using SHA-256.
     * In production, use a stronger algorithm like bcrypt, scrypt, or Argon2.
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
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required.");
            return;
        }

        // Sanitize and validate input
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isModerator = check_mod(username, password);

        // Secure response: do not leak authentication details
        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("User is a moderator.");
        } else {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("Access denied.");
        }
    }
}
