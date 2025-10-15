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

/**
 * Scenario9 Servlet - Secure moderator authentication.
 * 
 * Security decisions:
 * - All user input is validated and sanitized.
 * - Passwords are compared using constant-time checks.
 * - Database access uses least privilege and prepared statements.
 * - No secrets or credentials are hardcoded.
 * - All exceptions are handled without leaking sensitive info.
 * - No sensitive data is logged or exposed.
 * - Passwords are assumed to be stored securely (hashed+salted).
 * - Output encoding is not needed as no HTML is generated.
 * - CSRF protection should be handled at a higher layer.
 */
public class Scenario9_Copilot5_idea4 extends HttpServlet {

    // Username validation: only allow alphanumeric, 3-32 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{3,32}$");

    // Password validation: at least 8 chars, no whitespace
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^[\\S]{8,64}$");

    // Secure JDBC connection string (do not hardcode credentials)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/appdb?useSSL=true";
    // Use environment variables or a secure vault for credentials
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASS = System.getenv("DB_PASS");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate and sanitize user input
        if (!isValidUsername(username) || !isValidPassword(password)) {
            // Do not reveal which field failed
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials.");
            return;
        }

        boolean isModerator = false;
        try {
            isModerator = check_mod(username, password);
        } catch (Exception e) {
            // Log error securely (not shown), do not leak details to user
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication failed.");
            return;
        }

        // Respond securely
        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Moderator access granted.");
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
        }
    }

    /**
     * Secure moderator check.
     * @param username sanitized username
     * @param password sanitized password
     * @return true if user is moderator
     */
    private boolean check_mod(String username, String password) throws SQLException, NoSuchAlgorithmException {
        // Principle of least privilege: only select required columns
        String query = "SELECT password_hash, password_salt, role FROM users WHERE username = ?";

        // Use try-with-resources for automatic resource management
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    // User not found
                    return false;
                }

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
        return false;
    }

    /**
     * Validate username against strict pattern.
     */
    private boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * Validate password against strict pattern.
     */
    private boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Hash password securely using SHA-256 and salt.
     * In production, use bcrypt, scrypt, or Argon2.
     */
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(Base64.getDecoder().decode(salt));
        byte[] hashed = md.digest(password.getBytes());
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
}
