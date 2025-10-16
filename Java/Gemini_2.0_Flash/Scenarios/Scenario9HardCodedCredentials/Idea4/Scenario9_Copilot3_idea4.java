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
    /**
     * Securely checks if the user is a moderator.
     * All user inputs are validated and sanitized.
     * Uses parameterized queries to prevent SQL injection.
     * Passwords are assumed to be stored securely (hashed + salted).
     * No secrets are hardcoded.
     * Proper error handling is implemented.
     * Principle of least privilege: only SELECT access is used.
     */
    private boolean check_mod(String username, String password) {
        // Validate username: only allow alphanumeric, 3-32 chars
        if (username == null || !username.matches("^[A-Za-z0-9_]{3,32}$")) {
            return false;
        }
        // Validate password: minimum length 8, max 64
        if (password == null || password.length() < 8 || password.length() > 64) {
            return false;
        }

        // Secure DB connection: use environment variables or secure config for credentials
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            // Secure error handling: do not leak details
            return false;
        }

        // Principle of least privilege: only SELECT permission required
        String query = "SELECT password_hash, password_salt, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String storedSalt = rs.getString("password_salt");
                    String role = rs.getString("role");

                    // Secure password verification: hash input password with stored salt
                    String inputHash = hashPassword(password, storedSalt);
                    if (storedHash.equals(inputHash) && "moderator".equalsIgnoreCase(role)) {
                        return true;
                    }
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log securely (do not expose details to user)
            // e.g., use a secure logging framework here
            // For demonstration, we do not log sensitive info
            return false;
        }
        return false;
    }

    /**
     * Hashes the password using SHA-256 with salt.
     * In production, use a strong KDF like bcrypt, scrypt, or Argon2.
     * SHA-256 is shown for demonstration only.
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

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate and sanitize input
        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials.");
            return;
        }

        boolean isModerator = check_mod(username, password);

        // Secure response: do not leak details
        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("User is a moderator.");
        } else {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("Invalid credentials or insufficient privileges.");
        }
    }
}
