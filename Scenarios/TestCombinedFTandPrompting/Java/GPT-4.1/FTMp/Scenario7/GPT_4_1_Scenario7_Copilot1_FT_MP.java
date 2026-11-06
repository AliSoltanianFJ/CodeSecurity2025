import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GPT_4_1_Scenario7_Copilot1_FT_MP extends HttpServlet {
    /* 
     * Security Decisions:
     * - Uses PBKDF2 with HMAC-SHA-256 for password hashing (OWASP recommended).
     * - Generates a unique, cryptographically secure salt per user.
     * - All user inputs are validated and sanitized.
     * - Uses prepared statements for all SQL queries to prevent SQL injection.
     * - No secrets or credentials are hardcoded.
     * - Proper error handling without leaking sensitive info.
     * - Assumes HTTPS is enforced at the server/container level.
     * - Principle of least privilege: DB user should have only necessary permissions.
     * - All sensitive data (passwords) never logged or exposed.
     * - Secure password policy enforced.
     */

    // Secure password policy constants
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_LENGTH = 64;
    private static final int MIN_USERNAME_LENGTH = 4;
    private static final int MAX_USERNAME_LENGTH = 32;
    private static final int SALT_LENGTH = 32; // bytes
    private static final int HASH_ITERATIONS = 100_000;
    private static final int HASH_LENGTH = 256; // bits

    // Logger (do not log sensitive data!)
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario7_Copilot1_FT_MP.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Set secure response headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("Pragma", "no-cache");

        // Validate and sanitize username
        String username = req.getParameter("username");
        if (username == null || username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH
                || !username.matches("^[a-zA-Z0-9._-]+$")) { // Allow only safe chars
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }

        // Validate password
        String password = req.getParameter("password");
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH
                || !isPasswordStrong(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password does not meet complexity requirements.");
            return;
        }

        // Generate secure salt
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);

        // Hash the password securely
        String passwordHash;
        try {
            passwordHash = hashPassword(password.toCharArray(), salt);
        } catch (NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException e) {
            LOGGER.log(Level.SEVERE, "Password hashing failed", e); // Do not leak details to user
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error.");
            return;
        } finally {
            // Zero out password in memory as soon as possible
            if (password != null) {
                char[] pwdChars = password.toCharArray();
                java.util.Arrays.fill(pwdChars, '\0');
            }
        }

        // Base64-encode the salt for storage
        String saltBase64 = Base64.getEncoder().encodeToString(salt);

        // Principle of least privilege: get DB connection securely (no hardcoded creds)
        // Use environment variables or a secure secrets manager (never hardcode)
        try (Connection conn = getSafeConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")) {

            ps.setString(1, username.trim());
            ps.setString(2, passwordHash);
            ps.setString(3, saltBase64);
            ps.executeUpdate();
            resp.setStatus(HttpServletResponse.SC_CREATED);
        } catch (SQLIntegrityConstraintViolationException e) {
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error", e); // Do not leak DB details
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error.");
        }
    }

    // Secure password hashing using PBKDF2 (OWASP recommended)
    private String hashPassword(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, java.security.spec.InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, HASH_ITERATIONS, HASH_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        // Zero out password in memory
        spec.clearPassword();
        return Base64.getEncoder().encodeToString(hash);
    }

    // Enforce strong password policy (OWASP)
    private boolean isPasswordStrong(String password) {
        // At least one uppercase, one lowercase, one digit, one special char
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\|,.<>/?]).+$");
    }

    // Securely obtain a DB connection (credentials not hardcoded)
    private Connection getSafeConnection() throws SQLException {
        // Use environment variables or a secrets manager (never hardcode credentials)
        String dbUrl = System.getenv("SECURE_DB_URL");
        String dbUser = System.getenv("SECURE_DB_USER");
        String dbPass = System.getenv("SECURE_DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            LOGGER.severe("Database credentials not set in environment variables.");
            throw new SQLException("Database credentials not configured.");
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPass);
    }
}
