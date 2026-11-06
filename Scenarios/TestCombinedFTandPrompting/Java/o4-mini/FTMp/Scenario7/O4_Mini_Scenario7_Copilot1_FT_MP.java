import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.naming.Context;
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
 * Security measures:
 * - Input validation (username, password strength)
 * - PBKDF2WithHmacSHA256 with per-user salt and high iteration count
 * - Base64-encoded salt and hash for safe storage
 * - PreparedStatement to prevent SQL injection
 * - DataSource (connection pooling) via JNDI lookup
 * - try-with-resources for proper resource cleanup
 * - Generic error messages to avoid information leakage
 * - No hard-coded credentials
 * - HTTPS should be enforced at the container / proxy level
 */
public class O4_Mini_Scenario7_Copilot1_FT_MP extends HttpServlet {

    // PBKDF2 parameters
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH_BYTES = 16;          // 128-bit salt
    private static final int HASH_ITERATIONS = 100_000;       // OWASP recommends ≥100k
    private static final int HASH_KEY_LENGTH_BITS = 256;      // 256-bit derived key

    // Username/password policy
    private static final int USERNAME_MIN_LENGTH = 3;
    private static final int USERNAME_MAX_LENGTH = 30;
    private static final int PASSWORD_MIN_LENGTH = 12;

    // JNDI name of the DataSource
    private static final String DATASOURCE_JNDI = "java:comp/env/jdbc/MyAppDB";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce UTF-8 for request parameters
        req.setCharacterEncoding("UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1. Validate inputs
        if (username == null || password == null ||
            username.length() < USERNAME_MIN_LENGTH ||
            username.length() > USERNAME_MAX_LENGTH ||
            !username.matches("^[A-Za-z0-9_]+$") ||          // Only allow alphanumerics & underscore
            password.length() < PASSWORD_MIN_LENGTH) {

            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password.");
            return;
        }

        // 2. Generate per-user salt
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SecureRandom rng = new SecureRandom();
        rng.nextBytes(salt);

        // 3. Derive hash with PBKDF2
        byte[] hash;
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    HASH_ITERATIONS,
                    HASH_KEY_LENGTH_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            hash = skf.generateSecret(spec).getEncoded();
            // Immediately clear sensitive data
            spec.clearPassword();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // This should never happen if algorithm name is correct
            log("Password hashing failure", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "An internal error occurred.");
            return;
        }

        // 4. Store user in database
        //    Table schema assumption:
        //    CREATE TABLE users (
        //      id            SERIAL PRIMARY KEY,
        //      username      VARCHAR(30) UNIQUE NOT NULL,
        //      password_hash TEXT NOT NULL,
        //      password_salt TEXT NOT NULL,
        //      created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        //    );
        String insertSql = "INSERT INTO users (username, password_hash, password_salt) VALUES (?, ?, ?)";

        try {
            // Obtain DataSource via JNDI; container must define it in context.xml/web.xml
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup(DATASOURCE_JNDI);

            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertSql)) {

                // Principle of least privilege: use a DB user with only INSERT rights
                ps.setString(1, username);
                ps.setString(2, Base64.getEncoder().encodeToString(hash));
                ps.setString(3, Base64.getEncoder().encodeToString(salt));

                ps.executeUpdate();
            }
        } catch (NamingException ne) {
            log("JNDI DataSource lookup failed", ne);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "An internal configuration error occurred.");
            return;
        } catch (SQLException se) {
            // Unique constraint violation => username already exists
            String sqlState = se.getSQLState();
            if ("23505".equals(sqlState)) {  // PostgreSQL unique_violation
                resp.sendError(HttpServletResponse.SC_CONFLICT,
                               "Username already in use.");
            } else {
                log("Database error during user registration", se);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "An internal error occurred.");
            }
            return;
        }

        // 5. On success, redirect or respond accordingly
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().println("User registered successfully.");
    }
}

