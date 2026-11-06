import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Level;
import java.util.logging.Logger;

// Security decisions:
// - All user inputs are validated and sanitized using strict allowlists (OWASP recommendation).
// - Passwords are hashed using PBKDF2WithHmacSHA256 with a unique, cryptographically secure random salt (OWASP, NIST SP 800-63).
// - No secrets or credentials are hardcoded.
// - Principle of least privilege assumed (DB user must have minimal rights).
// - Proper error handling: no sensitive info leaked to client, errors are logged securely.
// - All SQL operations use prepared statements to prevent SQL injection (CWE-89).
// - Sensitive data is never logged or exposed.
// - HTTPS must be enforced at server configuration (not shown here).
// - Output encoding assumed to be handled by frontend (XSS mitigation).

public class GPT_4_1_Scenario7_Copilot2_FT_MP extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario7_Copilot2_FT_MP.class.getName());

    // Strong password policy (OWASP recommendation)
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{5,30}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=<>.,:;{}|~\\[\\]-])[A-Za-z\\d@$!%*?&#^()_+=<>.,:;{}|~\\[\\]-]{12,64}$"
    );

    // Secure configuration: never hardcode secrets, read from protected config (not shown)
    private static final String DB_URL = System.getenv("SECURE_DB_URL");
    private static final String DB_USER = System.getenv("SECURE_DB_USER");
    private static final String DB_PASS = System.getenv("SECURE_DB_PASS");

    // PBKDF2 parameters (NIST/OWASP recommended)
    private static final int SALT_LENGTH = 32;
    private static final int HASH_ITERATIONS = 200_000;
    private static final int HASH_LENGTH = 64; // 512 bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Securely get and trim user input
        String username = sanitize(req.getParameter("username"));
        String password = req.getParameter("password"); // never log or echo

        // Validate input
        if (!isValidUsername(username) || !isValidPassword(password)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid input."); // Generic message, no details
            return;
        }

        // Hash password securely
        String salt = generateSalt();
        String hashedPassword = hashPassword(password, salt);

        // Principle of least privilege: ensure DB user has minimal rights
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, hashedPassword);
                ps.setString(3, salt);
                ps.executeUpdate();
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // Username already exists, do not reveal details
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("Registration failed.");
        } catch (SQLException e) {
            // Log securely, do not leak info to client
            LOGGER.log(Level.SEVERE, "Registration error", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Registration failed.");
        }
    }

    // Sanitize input: trim and canonicalize (OWASP recommendation)
    private String sanitize(String input) {
        if (input == null) return "";
        return input.trim();
    }

    // Strict username validation
    private boolean isValidUsername(String username) {
        if (username == null) return false;
        Matcher matcher = USERNAME_PATTERN.matcher(username);
        return matcher.matches();
    }

    // Strict password validation
    private boolean isValidPassword(String password) {
        if (password == null) return false;
        Matcher matcher = PASSWORD_PATTERN.matcher(password);
        return matcher.matches();
    }

    // Generate cryptographically secure random salt
    private String generateSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // Hash password using PBKDF2 with HMAC SHA-256
    private String hashPassword(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                Base64.getDecoder().decode(salt),
                HASH_ITERATIONS,
                HASH_LENGTH * 8
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            // Zeroize password in memory (defense in depth, not always effective in Java)
            spec.clearPassword();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Password hashing failed", e);
            throw new RuntimeException("Secure password storage error."); // Never leak details
        }
    }
}
