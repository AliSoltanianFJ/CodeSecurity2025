import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class O4_Mini_Scenario7_Copilot9_FT_MP extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario7.class.getName());

    // PBKDF2 parameters
    private static final int SALT_LENGTH = 16;       // 16 bytes = 128 bits
    private static final int HASH_ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;       // 256 bits

    // JNDI name of the DataSource (configured in your application server)
    private static final String DATASOURCE_JNDI = "java:comp/env/jdbc/MyDB";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS transport
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required");
            return;
        }

        // Retrieve and trim inputs
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Basic null checks
        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required");
            return;
        }
        username = username.trim();

        // Validate username: 3–32 characters, only letters, digits, underscore
        if (!username.matches("^[A-Za-z0-9_]{3,32}$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format");
            return;
        }

        // Validate password strength: at least 8 chars, upper, lower, digit, special
        if (password.length() < 8
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*\\d.*")
                || !password.matches(".*[!@#$%^&*()_+\\-=[\\]{};':\"\\\\|,.<>/?].*")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Password must be at least 8 characters long and include upper, lower, digit, and special");
            return;
        }

        char[] passwordChars = password.toCharArray();
        Arrays.fill(password.toCharArray(), '\0');  // overwrite raw String (best‐effort)

        try {
            // Generate a per‐user random salt
            byte[] salt = generateSalt();

            // Hash the password with PBKDF2 + HMAC-SHA256
            String passwordHash = hashPassword(passwordChars, salt);

            // Zero out password chars immediately after hashing
            Arrays.fill(passwordChars, '\0');

            // Encode salt for storage
            String saltB64 = Base64.getEncoder().encodeToString(salt);

            // Store user record
            storeUser(username, passwordHash, saltB64, HASH_ITERATIONS);

            // On success, return 201 Created
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("User registered successfully");

        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            // Cryptography errors should rarely happen; log and return generic 500
            logger.log(Level.SEVERE, "Server error during password hashing", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
        } catch (SQLIntegrityConstraintViolationException e) {
            // Username already exists
            logger.log(Level.WARNING, "Registration attempt with existing username: {0}", username);
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already in use");
        } catch (NamingException | SQLException e) {
            // Database or JNDI lookup error
            logger.log(Level.SEVERE, "Server error during user registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    /**
     * Performs a JNDI lookup to retrieve the configured DataSource.
     * This avoids hard-coding credentials in code.
     */
    private DataSource getDataSource() throws NamingException {
        InitialContext ctx = new InitialContext();
        return (DataSource) ctx.lookup(DATASOURCE_JNDI);
    }

    /**
     * Stores the new user record in the database using a parameterized query.
     * Applies least privilege: the DB user in the DataSource should only have INSERT rights.
     */
    private void storeUser(String username,
                           String passwordHash,
                           String saltB64,
                           int iterations)
            throws NamingException, SQLException {

        String sql = "INSERT INTO users (username, password_hash, salt, iterations) VALUES (?, ?, ?, ?)";

        try (Connection conn = getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, saltB64);
            ps.setInt(4, iterations);

            ps.executeUpdate();
        }
    }

    /**
     * Generates a cryptographically secure random salt.
     */
    private static byte[] generateSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);
        return salt;
    }

    /**
     * Hashes the password using PBKDF2 with HMAC-SHA256.
     * Returns the Base64-encoded hash.
     */
    private static String hashPassword(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        PBEKeySpec spec = new PBEKeySpec(password, salt, HASH_ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }
}

