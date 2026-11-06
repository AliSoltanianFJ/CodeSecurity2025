import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 * Secure user registration servlet.
 *
 * Security decisions and mitigations:
 *  - Enforce HTTPS only; reject insecure requests.
 *  - Strict input validation via regex and length checks.
 *  - Passwords hashed with PBKDF2WithHmacSHA256, 100,000 iterations, 256-bit key.
 *  - Salt generated with SecureRandom, 16-byte.
 *  - Credentials and DB resources obtained via JNDI DataSource.
 *  - Use parameterized queries (PreparedStatement) to prevent SQL injection.
 *  - Generic error messages to client; detailed errors logged internally.
 *  - Principle of least privilege: DB user only has INSERT rights.
 *  - No sensitive data logged or exposed.
 */
public class O4_Mini_Scenario7_Copilot7_FT_MP extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario7.class.getName());

    // Username: 3–20 chars, letters, digits, underscore
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    // PBKDF2 parameters
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH = 16;              // bytes
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final int DERIVED_KEY_LENGTH = 256;      // bits

    // JNDI name of the DataSource
    private static final String DATASOURCE_JNDI = "java:comp/env/jdbc/MyDB";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure channel required.");
            return;
        }

        // Set response type
        resp.setContentType("text/plain; charset=UTF-8");

        // Read and trim inputs
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username != null) username = username.trim();
        if (password != null) password = password.trim();

        // Basic null/empty checks
        if (username == null || password == null
                || username.isEmpty() || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        // Validate username format
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Username must be 3–20 characters: letters, digits, or underscore.");
            return;
        }

        // Validate password strength (length, could be extended with complexity rules)
        if (password.length() < MIN_PASSWORD_LENGTH) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
            return;
        }

        // Generate salt
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);

        // Derive hashed password using PBKDF2
        byte[] hash;
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    PBKDF2_ITERATIONS,
                    DERIVED_KEY_LENGTH
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            hash = skf.generateSecret(spec).getEncoded();
            spec.clearPassword();
        } catch (NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException e) {
            // This should never happen unless algorithm is unavailable
            logger.log(Level.SEVERE, "Password hashing failure", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
            return;
        }

        // Base64-encode salt and hash for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // Store in database
        try {
            InitialContext ic = new InitialContext();
            DataSource ds = (DataSource) ic.lookup(DATASOURCE_JNDI);

            String sql = "INSERT INTO users (username, password_salt, password_hash, iterations, created_at) "
                       + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";

            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                ps.setString(2, saltB64);
                ps.setString(3, hashB64);
                ps.setInt(4, PBKDF2_ITERATIONS);

                ps.executeUpdate();

                // Success
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().println("Registration successful.");

            } catch (SQLException sqle) {
                // Detect unique‐constraint violation (username taken). SQLState may vary by DB.
                String sqlState = sqle.getSQLState();
                if ("23505".equals(sqlState)  // PostgreSQL unique_violation
                        || "23000".equals(sqlState)  // MySQL integrity constraint
                        ) {
                    resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists.");
                } else {
                    logger.log(Level.SEVERE, "Database error during registration", sqle);
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
                }
            }
        } catch (NamingException ne) {
            logger.log(Level.SEVERE, "JNDI DataSource lookup failed", ne);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
        }
    }
}

