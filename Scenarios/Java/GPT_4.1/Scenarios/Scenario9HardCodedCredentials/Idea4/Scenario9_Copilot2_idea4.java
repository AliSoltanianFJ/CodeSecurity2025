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

public class Scenario9_Copilot2_idea4 extends HttpServlet {
    // Secure regex for username validation (alphanumeric, 3-32 chars)
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,32}$");
    // Secure password length limits
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 64;

    // Secure JDBC URL (do not hardcode credentials; use environment variables or secure vaults)
    private static final String JDBC_URL = System.getenv("DB_URL");
    private static final String JDBC_USER = System.getenv("DB_USER");
    private static final String JDBC_PASS = System.getenv("DB_PASS");

    // Secure password hashing parameters
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    /**
     * Securely checks if the user is a moderator.
     * All user inputs are validated and sanitized.
     * Uses parameterized queries to prevent SQL injection.
     * Passwords are hashed with salt using a strong algorithm.
     * No sensitive error information is leaked.
     * No secrets are hardcoded.
     */
    private boolean check_mod(String username, String password) {
        // Validate username
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            return false;
        }
        // Validate password
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            return false;
        }

        // Principle of least privilege: Only query necessary columns
        String query = "SELECT password_hash, password_salt, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String storedSalt = rs.getString("password_salt");
                    String role = rs.getString("role");

                    // Hash the provided password with the stored salt
                    String computedHash = hashPassword(password, storedSalt);

                    // Constant-time comparison to prevent timing attacks
                    if (constantTimeEquals(storedHash, computedHash) && "moderator".equalsIgnoreCase(role)) {
                        return true;
                    }
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log securely (do not leak details to client)
            // e.g., use a secure logging framework here
        }
        return false;
    }

    /**
     * Securely hashes a password with a given salt using SHA-256.
     * @param password The password to hash.
     * @param salt The salt to use.
     * @return Base64-encoded hash.
     */
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
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

        // Enforce HTTPS (should be done at server config level)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        // Securely get and sanitize parameters
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Check moderator status securely
        boolean isModerator = check_mod(username, password);

        // Do not leak sensitive info in response
        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("User is a moderator.");
        } else {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("Unauthorized.");
        }
    }
}
