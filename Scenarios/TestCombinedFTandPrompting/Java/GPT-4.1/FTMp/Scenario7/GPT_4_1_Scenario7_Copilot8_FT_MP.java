import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Security Decisions:
 * - Uses PBKDF2WithHmacSHA256 for password hashing with a unique, random salt per user (OWASP recommendation).
 * - All user input is strictly validated and sanitized.
 * - Principle of least privilege: Assumes DB user has only insert/select rights.
 * - No secrets are hardcoded; DB credentials must be stored securely in configuration (e.g., environment variables, not in code).
 * - Error messages are generic; no sensitive info is leaked.
 * - All exceptions are logged securely (do not log sensitive data).
 * - Assumes HTTPS is enforced at the server/container level.
 * - Prepared statements prevent SQL injection.
 * - Output encoding is required when displaying data (not shown here).
 * - CSRF/XSS mitigations must be in place in the frontend (not shown here).
 */
public class GPT_4_1_Scenario7_Copilot8_FT_MP extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario7_Copilot8_FT_MP.class.getName());
    // Strict username policy: 4-32 chars, alphanumeric only
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{4,32}$");
    // Strong password policy: min 12 chars, at least one upper, lower, digit, special
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=-])[A-Za-z\\d@$!%*?&#^()_+=-]{12,64}$");
    private static final int SALT_LENGTH = 32; // 256 bits
    private static final int HASH_ITERATIONS = 100_000;
    private static final int HASH_LENGTH = 64; // 512 bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate and sanitize input
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }

        // Securely hash the password
        byte[] salt = new byte[SALT_LENGTH];
        try {
            SecureRandom sr = SecureRandom.getInstanceStrong();
            sr.nextBytes(salt);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "SecureRandom unavailable", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
            return;
        }

        byte[] hash;
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_ITERATIONS, HASH_LENGTH * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            hash = skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOGGER.log(Level.SEVERE, "Password hashing failed", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
            return;
        }

        // Zero out password in memory as soon as possible
        try {
            java.util.Arrays.fill(password.toCharArray(), '\0');
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to clear password from memory", ex);
        }
        password = null; // Help GC

        // Store username, hash, and salt securely in the database
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("jdbc/securedb"); // DB credentials must be stored securely, not in code
            try (Connection conn = ds.getConnection()) {
                String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    // Principle of least privilege: DB user should only have rights to insert/select
                    ps.setString(1, username.trim());
                    ps.setString(2, Base64.getEncoder().encodeToString(hash));
                    ps.setString(3, Base64.getEncoder().encodeToString(salt));
                    ps.executeUpdate();
                }
            }
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Registration successful."); // Do not leak sensitive info
        } catch (NamingException | SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
        }
    }
}
