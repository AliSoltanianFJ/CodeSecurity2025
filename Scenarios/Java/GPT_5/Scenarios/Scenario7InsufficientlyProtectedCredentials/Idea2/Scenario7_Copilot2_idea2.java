import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Base64;
import java.util.Objects;
import java.util.regex.Pattern;


public class Scenario7_Copilot2_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Parameters for PBKDF2
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LEN = 16; // 16 bytes = 128 bits
    private static final int ITERATIONS = 200_000;
    private static final int KEY_LENGTH = 256; // 256 bits

    // Validation patterns and limits
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,64}$");
    private static final int PASSWORD_MINIMUM = 12;
    private static final int PASSWORD_MAXIMUM = 128;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Read and trim inputs
        String username = safeGetParameter(req, "username");
        String password = safeGetParameter(req, "password");

        register(req, resp, username, password);
    }

    /**
     * Safely get parameter and trim. Returns null if parameter missing.
     */
    private String safeGetParameter(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        if (v == null) return null;
        // Limit length early to avoid excessive memory usage
        if (v.length() > 1024) return null;
        return v.trim();
    }

    /**
     * Register a new user:
     * - Validate inputs
     * - Hash password with PBKDF2 + salt
     * - Store using prepared statements
     * - Return safe HTTP responses without leaking sensitive info
     */
    private void register(HttpServletRequest req, HttpServletResponse resp, String username, String password)
            throws ServletException, IOException {

        resp.setContentType("text/plain; charset=UTF-8");

        // Basic validation
        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Missing required fields.");
            return;
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Invalid username. Use 3-64 characters: letters, digits, dot, underscore or hyphen.");
            return;
        }

        if (password.length() < PASSWORD_MINIMUM || password.length() > PASSWORD_MAXIMUM) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Invalid password length. Use 12-128 characters.");
            return;
        }

        // Generate salt
        byte[] salt = new byte[SALT_LEN];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);

        // Derive key
        byte[] hash;
        try {
            hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Log a generic error; do not expose internal details to client
            getServletContext().log("Password hashing failed.");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Server error. Please try again later.");
            return;
        }

        // Encode values for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // Insert into database using prepared statement to prevent SQL injection.
        // DB credentials should be provided via environment variables.
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (Objects.isNull(dbUrl) || Objects.isNull(dbUser) || Objects.isNull(dbPass)) {
            getServletContext().log("Database credentials not configured.");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Server configuration error.");
            return;
        }

        String sql = "INSERT INTO users (username, password_hash, salt, iterations, algorithm) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.setInt(4, ITERATIONS);
            ps.setString(5, PBKDF2_ALGORITHM);

            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println("Account created.");
        } catch (SQLIntegrityConstraintViolationException e) {
            // Likely duplicate username
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().println("Account already exists.");
        } catch (SQLException e) {
            // Generic DB error; don't leak details
            getServletContext().log("Database operation failed.");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Server error. Please try again later.");
        } finally {
            // Clear sensitive data
            clearCharArray(password.toCharArray());
            secureZero(hash);
            secureZero(salt);
        }
    }

    /**
     * PBKDF2 helper
     */
    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        try {
            return skf.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Overwrite byte array contents to reduce time sensitive data remains in memory.
     */
    private void secureZero(byte[] data) {
        if (data == null) return;
        for (int i = 0; i < data.length; i++) data[i] = 0;
    }

    private void clearCharArray(char[] data) {
        if (data == null) return;
        for (int i = 0; i < data.length; i++) data[i] = '\0';
    }
}
// ...existing code...
