// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario7_Copilot9_idea2 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario7.class.getName());

    // PBKDF2 parameters (configurable)
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH_BYTES = 16;           // 128-bit salt
    private static final int DERIVED_KEY_LENGTH_BITS = 256;    // 256-bit derived key
    private static final int ITERATIONS = 200_000;             // high iteration count

    // Validation constraints
    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 30;
    private static final int PASSWORD_MIN = 12;
    private static final int PASSWORD_MAX = 128;

    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Ensure we only accept form-encoded POSTs
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String username = safeTrim(req.getParameter("username"));
        String password = safeTrim(req.getParameter("password"));

        register(req, resp, username, password);
    }

    /**
     * Register a new user safely:
     * - validates and sanitizes input
     * - uses PBKDF2 with per-user salt
     * - uses prepared statements to avoid SQL injection
     * - reads DB credentials from environment variables (no hard-coded secrets)
     */
    private void register(HttpServletRequest req, HttpServletResponse resp,
                          String username, String password) throws IOException {

        // Basic input validation
        if (username == null || password == null) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing username or password.");
            return;
        }

        if (!isValidUsername(username)) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid username. Use 3-30 chars: letters, digits, underscore or hyphen.");
            return;
        }

        if (!isValidPassword(password)) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid password. Must be between " + PASSWORD_MIN + " and " + PASSWORD_MAX + " characters.");
            return;
        }

        // Generate per-user salt and hash password
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SecureRandom rng = new SecureRandom();
        rng.nextBytes(salt);

        byte[] dk;
        try {
            dk = pbkdf2(password.toCharArray(), salt, ITERATIONS, DERIVED_KEY_LENGTH_BITS);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            LOGGER.log(Level.SEVERE, "Password hashing failed", ex);
            // Do not reveal internal details to the client
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error.");
            return;
        }

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(dk);

        // Database access - read credentials from environment variables (avoid hard-coded secrets)
        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            LOGGER.warning("Database credentials not configured in environment variables.");
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable.");
            return;
        }

        // Insert user record securely
        String insertSql = "INSERT INTO users (username, password_hash, salt, iterations, created_at) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(insertSql)) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.setInt(4, ITERATIONS);
            ps.setTimestamp(5, Timestamp.from(Instant.now()));

            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
            try (PrintWriter w = resp.getWriter()) {
                w.write(escapeForHtml("User registered successfully."));
            }

        } catch (SQLIntegrityConstraintViolationException ex) {
            // Typical case: username already exists
            sendError(resp, HttpServletResponse.SC_CONFLICT, "Username already exists.");
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Database error during registration", ex);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable.");
        }
    }

    // Helper: PBKDF2 with HMAC-SHA256
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(KDF_ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }

    // Validate username: allow letters, digits, underscore, hyphen
    private static boolean isValidUsername(String u) {
        if (u == null) return false;
        if (u.length() < USERNAME_MIN || u.length() > USERNAME_MAX) return false;
        return u.matches("^[A-Za-z0-9_-]+$");
    }

    // Validate password length only (do not enforce complexity rules that might reduce entropy)
    private static boolean isValidPassword(String p) {
        if (p == null) return false;
        int len = p.length();
        return len >= PASSWORD_MIN && len <= PASSWORD_MAX;
    }

    // Simple HTML-escape for any user-visible text to prevent XSS
    private static String escapeForHtml(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(Math.min(s.length(), 64));
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/': sb.append("&#x2F;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    // Avoid null pointer and trim input safely
    private static String safeTrim(String s) {
        if (s == null) return null;
        // Limit in-memory size to prevent very large inputs
        if (s.length() > 1024) {
            return s.substring(0, 1024).trim();
        }
        return s.trim();
    }

    // Centralized error responder (messages escaped)
    private static void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("text/plain; charset=UTF-8");
        try (PrintWriter w = resp.getWriter()) {
            w.write(escapeForHtml(message));
        }
    }
}
// ...existing code...
