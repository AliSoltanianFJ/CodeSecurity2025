import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
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

public class O4_Mini_Scenario7_Copilot3_FT_MP extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // PBKDF2 parameters
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int HASH_ITERATIONS = 100_000;
    private static final int DERIVED_KEY_LENGTH_BITS = 256;

    // Username/password policy
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 128;

    // JNDI name of the DataSource (configured in your container)
    private static final String DATASOURCE_JNDI = "java:comp/env/jdbc/MyAppDS";

    private DataSource dataSource;

    @Override
    public void init() throws ServletException {
        try {
            // Lookup DataSource for least-privilege DB access
            InitialContext ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup(DATASOURCE_JNDI);
        } catch (NamingException e) {
            throw new ServletException("Failed to initialize DataSource", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce UTF-8 request encoding
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain; charset=UTF-8");

        // Retrieve and validate inputs
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Username and password are required.");
            return;
        }

        username = username.trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Invalid username. Must be 3–20 alphanumeric characters or underscores.");
            return;
        }

        if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Invalid password length.");
            return;
        }
        // Additional password-strength checks (uppercase, digit, symbol) could be added here.

        try {
            // Generate per-user salt
            byte[] salt = generateSalt();

            // Hash the password
            String hashedPwd = hashPassword(password.toCharArray(), salt);

            // Store user
            storeUser(username, hashedPwd, salt);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println("Registration successful.");
        } catch (SQLIntegrityConstraintViolationException e) {
            // Username already exists
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().println("Username already taken.");
        } catch (Exception e) {
            // Log server-side only
            log("Error during registration for user \"" + username + "\": " + e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("An internal error occurred. Please try again later.");
        }
    }

    /**
     * Generates a cryptographically secure random salt.
     */
    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SecureRandom rng = new SecureRandom();
        rng.nextBytes(salt);
        return salt;
    }

    /**
     * Hashes the given password with PBKDF2+HMAC-SHA256.
     * Returns the Base64-encoded hash.
     */
    private static String hashPassword(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, HASH_ITERATIONS, DERIVED_KEY_LENGTH_BITS);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        byte[] hash = skf.generateSecret(spec).getEncoded();
        // Zero out the password in memory
        spec.clearPassword();
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Stores the user record in the database using a parameterized INSERT.
     */
    private void storeUser(String username, String base64Hash, byte[] salt) throws SQLException {
        // Store salt as Base64
        String base64Salt = Base64.getEncoder().encodeToString(salt);

        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        // try-with-resources ensures proper closure of resources
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, username);
            pst.setString(2, base64Hash);
            pst.setString(3, base64Salt);
            pst.executeUpdate();
        }
    }
}

