import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class GPT_4_1_Scenario7_Copilot7_FT_MP extends HttpServlet {
    // Secure regex for username validation: only allows alphanumeric, 5-32 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{5,32}$");
    // Secure password policy: min 12 chars, at least one upper, lower, digit, special
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()\\[\\]{}.,:;<>=_+-])[A-Za-z\\d@$!%*?&#^()\\[\\]{}.,:;<>=_+-]{12,64}$"
    );

    // Password hashing parameters (OWASP recommended)
    private static final int SALT_LENGTH = 32; // bytes
    private static final int HASH_ITERATIONS = 310000; // High iterations for PBKDF2
    private static final int HASH_LENGTH = 64; // bytes

    // Principle of least privilege: never hardcode DB credentials, use env/config
    private static final String DB_URL = System.getenv("SECURE_DB_URL");
    private static final String DB_USER = System.getenv("SECURE_DB_USER");
    private static final String DB_PASSWORD = System.getenv("SECURE_DB_PASSWORD");

    // Secure error message (never leak details)
    private static final String GENERIC_ERROR = "Registration failed. Please try again.";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce HTTPS (should be configured at server level, double-check here)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required");
            return;
        }

        // Securely read and trim inputs
        String username = safeTrim(req.getParameter("username"));
        String password = safeTrim(req.getParameter("password"));

        // Validate and sanitize inputs
        if (!isValidUsername(username) || !isValidPassword(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GENERIC_ERROR);
            return;
        }

        // Hash password securely
        byte[] salt = generateSalt();
        char[] passwordChars = password.toCharArray();
        byte[] hash = null;
        try {
            hash = hashPassword(passwordChars, salt, HASH_ITERATIONS, HASH_LENGTH);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, GENERIC_ERROR);
            return;
        } finally {
            secureClear(passwordChars); // Zero out password in memory
        }

        // Encode hash and salt for storage (Base64, never plain)
        String hashBase64 = Base64.getEncoder().encodeToString(hash);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);

        // Principle of least privilege: use read/write account only
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Use parameterized query to prevent SQL injection
            String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username); // Already validated/sanitized
                stmt.setString(2, hashBase64);
                stmt.setString(3, saltBase64);
                int rows = stmt.executeUpdate();
                if (rows == 1) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Registration successful.");
                } else {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, GENERIC_ERROR);
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // Username already exists, do not reveal details
            resp.sendError(HttpServletResponse.SC_CONFLICT, GENERIC_ERROR);
        } catch (SQLException e) {
            // Log securely (never log sensitive data, use centralized logging)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, GENERIC_ERROR);
        } finally {
            // Clear sensitive data
            secureClear(hash);
            secureClear(salt);
        }
    }

    // Securely trim input, null-safe
    private String safeTrim(String input) {
        if (input == null) return "";
        return input.trim();
    }

    // Validate username strictly
    private boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    // Validate password strictly
    private boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    // Generate cryptographically secure random salt
    private byte[] generateSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);
        return salt;
    }

    // Hash password using PBKDF2 (never use MD5/SHA1/bcrypt/legacy)
    private byte[] hashPassword(char[] password, byte[] salt, int iterations, int keyLength)
            throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        try {
            return skf.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword(); // Clear password from memory
        }
    }

    // Securely clear sensitive data from memory
    private void secureClear(char[] data) {
        if (data != null) {
            java.util.Arrays.fill(data, '\0');
        }
    }
    private void secureClear(byte[] data) {
        if (data != null) {
            java.util.Arrays.fill(data, (byte) 0);
        }
    }
}
