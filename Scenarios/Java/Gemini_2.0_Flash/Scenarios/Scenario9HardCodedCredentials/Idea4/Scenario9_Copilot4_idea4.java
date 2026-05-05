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

public class Scenario9_Copilot4_idea4 extends HttpServlet {
    // Secure JDBC connection string should be loaded from environment/config, not hardcoded.
    private static final String DB_URL = System.getenv("SCENARIO9_DB_URL");
    private static final String DB_USER = System.getenv("SCENARIO9_DB_USER");
    private static final String DB_PASS = System.getenv("SCENARIO9_DB_PASS");

    /**
     * Securely checks if the user is a moderator.
     * All user inputs are validated and sanitized.
     * Passwords are compared using constant-time checks.
     * Uses prepared statements to prevent SQL injection.
     * Never leaks sensitive error details to the client.
     */
    private boolean check_mod(String username, String password) {
        // Validate username: only allow alphanumeric, 3-32 chars
        if (username == null || !username.matches("^[A-Za-z0-9]{3,32}$")) {
            return false;
        }
        // Validate password: minimum length 8, max 64, no control chars
        if (password == null || password.length() < 8 || password.length() > 64 ||
            !password.matches("^[\\x21-\\x7E]+$")) {
            return false;
        }

        // Principle of least privilege: only query required columns
        String sql = "SELECT password_hash, password_salt, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return false; // User not found
                }
                String storedHash = rs.getString("password_hash");
                String storedSalt = rs.getString("password_salt");
                String role = rs.getString("role");

                // Secure password verification using SHA-256 and salt
                String computedHash = hashPassword(password, storedSalt);
                if (!constantTimeEquals(storedHash, computedHash)) {
                    return false;
                }
                // Only return true if user is a moderator
                return "moderator".equalsIgnoreCase(role);
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log securely, do not leak details to client
            log("Authentication error", e);
            return false;
        }
    }

    /**
     * Hashes password securely using SHA-256 and salt.
     * @param password Plain password
     * @param salt Base64-encoded salt
     * @return Base64-encoded hash
     */
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        md.update(saltBytes);
        byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
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

        // Enforce HTTPS (should be done at server config, but double-check here)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Do not leak details in response
        boolean isModerator = check_mod(username, password);
        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Access granted.");
        } else {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("Access denied.");
        }
    }
}
