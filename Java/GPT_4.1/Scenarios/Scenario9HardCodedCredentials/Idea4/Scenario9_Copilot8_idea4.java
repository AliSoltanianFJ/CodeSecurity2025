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

public class Scenario9_Copilot8_idea4 extends HttpServlet {
    // Secure regex for username validation (alphanumeric, 3-32 chars)
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,32}$");
    // Secure password length requirement
    private static final int MIN_PASSWORD_LENGTH = 8;
    // Database connection string should be securely loaded from environment/config, not hardcoded
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASS = System.getenv("DB_PASS");

    /**
     * Securely checks if the user is a moderator.
     * All inputs are validated and sanitized.
     * Uses parameterized queries to prevent SQL injection.
     * Passwords are assumed to be stored as salted hashes.
     * No sensitive information is leaked in error messages.
     * No secrets are hardcoded.
     */
    private boolean check_mod(String username, String password) {
        // Validate username
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            return false;
        }
        // Validate password
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }

        // Principle of least privilege: only query necessary columns
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
                    if (MessageDigest.isEqual(storedHash.getBytes(), computedHash.getBytes())) {
                        // Only return true if user is a moderator
                        return "moderator".equalsIgnoreCase(role);
                    }
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log error securely (do not expose details to user)
            // e.g., use a secure logging framework here
        }
        return false;
    }

    /**
     * Secure password hashing using SHA-256 and salt.
     * In production, use a strong algorithm like bcrypt, scrypt, or Argon2.
     */
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt.getBytes());
        byte[] hashed = md.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hashed);
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
        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials.");
            return;
        }

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
