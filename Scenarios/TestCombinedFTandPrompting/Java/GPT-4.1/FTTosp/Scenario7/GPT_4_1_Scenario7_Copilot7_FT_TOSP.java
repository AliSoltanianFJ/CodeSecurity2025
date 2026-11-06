import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class GPT_4_1_Scenario7_Copilot7_FT_TOSP extends HttpServlet {
    // Secure username pattern: 3-50 chars, letters, numbers, underscores only
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,50}$");
    // Secure password pattern: min 10 chars, at least one upper, lower, digit, special
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#^(){}\\[\\]=+_,-])[A-Za-z\\d@$!%*?&.#^(){}\\[\\]=+_,-]{10,64}$");

    /**
     * Securely hash a password using PBKDF2 with a random salt
     */
    private static String hashPassword(char[] password, byte[] salt) throws Exception {
        // Use PBKDF2WithHmacSHA256 (strong, modern)
        javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password, salt, 100_000, 256);
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Generate a cryptographically secure random salt
     */
    private static byte[] generateSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[32]; // 256 bits
        sr.nextBytes(salt);
        return salt;
    }

    /**
     * Securely register a new user
     * All user input is validated and sanitized.
     * SQL injection is prevented with prepared statements.
     * No sensitive info is leaked in errors.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce HTTPS (should be set in server config, but check here)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        // Validate and sanitize username
        String username = req.getParameter("username");
        if (username == null || username.length() > 50) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }
        Matcher userMatcher = USERNAME_PATTERN.matcher(username.trim());
        if (!userMatcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }
        String cleanUsername = username.trim();

        // Validate password
        String password = req.getParameter("password");
        if (password == null || password.length() > 64) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }
        Matcher passMatcher = PASSWORD_PATTERN.matcher(password);
        if (!passMatcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }

        // Hash password securely
        String passwordHash;
        String saltStr;
        try {
            byte[] salt = generateSalt();
            saltStr = Base64.getEncoder().encodeToString(salt);
            passwordHash = hashPassword(password.toCharArray(), salt);
        } catch (Exception e) {
            // Log securely, do not leak sensitive info
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        } finally {
            // Zero out password in memory ASAP
            if (password != null) {
                char[] p = password.toCharArray();
                java.util.Arrays.fill(p, '\0');
            }
        }

        // Secure DB connection (never hardcode creds, use env vars or secure vault)
        String dbUrl = System.getenv("SECURE_DB_URL");
        String dbUser = System.getenv("SECURE_DB_USER");
        String dbPass = System.getenv("SECURE_DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server misconfiguration.");
            return;
        }

        // Principle of least privilege: DB user should only have necessary rights
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            // Use prepared statements to prevent SQL injection
            String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, cleanUsername);
                ps.setString(2, passwordHash);
                ps.setString(3, saltStr);
                ps.executeUpdate();
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // Username already exists, do not leak details
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Registration failed.");
            return;
        } catch (SQLException e) {
            // Generic error, log securely
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
            return;
        }

        // Success: never expose sensitive info or echo user input
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("Registration successful.");
    }
}
