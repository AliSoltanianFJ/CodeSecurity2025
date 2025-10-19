// ...existing code...
import io.javalin.Javalin;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario7_Copilot6_idea1 extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(Scenario7.class.getName());

    // PBKDF2 parameters (use strong iteration count)
    private static final int ITERATIONS = 200_000;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_LENGTH = 16; // bytes

    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username or password");
            return;
        }

        // convert password to char[] so it can be cleared from memory
        char[] pwdChars = password.toCharArray();
        try {
            try {
                register(username, pwdChars);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().println("User registered");
            } catch (SQLIntegrityConstraintViolationException e) {
                // duplicate username
                resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists");
            } catch (ClassNotFoundException e) {
                LOG.log(Level.SEVERE, "JDBC driver not found", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Database error during registration", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed");
            } catch (GeneralSecurityException e) {
                LOG.log(Level.SEVERE, "Cryptographic failure during registration", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed");
            }
        } finally {
            // zero out password characters for best-effort memory hygiene
            for (int i = 0; i < pwdChars.length; i++) {
                pwdChars[i] = 0;
            }
        }
    }

    /**
     * Securely derives a password hash and stores username, hash and salt.
     * Uses PBKDF2WithHmacSHA256 with a per-user random salt.
     *
     * IMPORTANT:
     * - Database connection details are read from environment variables:
     *   DB_URL, DB_USER, DB_PASSWORD. If not set, reasonable defaults are used.
     *
     * @param username non-null username
     * @param passwordChars password as char[] (will not be modified by this method)
     * @throws GeneralSecurityException on crypto errors
     * @throws SQLException on DB errors
     * @throws ClassNotFoundException if JDBC driver class cannot be found
     */
    private void register(String username, char[] passwordChars)
            throws GeneralSecurityException, SQLException, ClassNotFoundException {

        // Generate random salt
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);

        // Derive key using PBKDF2WithHmacSHA256
        PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, ITERATIONS, KEY_LENGTH);
        byte[] derivedKey;
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            derivedKey = skf.generateSecret(spec).getEncoded();
        } finally {
            // clear sensitive data in PBEKeySpec
            spec.clearPassword();
        }

        // Base64 encode for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(derivedKey);

        // Read DB connection details from environment (safer than hard-coding)
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=UTC");
        String dbUser = System.getenv().getOrDefault("DB_USER", "appuser");
        String dbPass = System.getenv().getOrDefault("DB_PASSWORD", "change_me");

        // Ensure JDBC driver is available (adjust driver class if using a different DB)
        // For MySQL: com.mysql.cj.jdbc.Driver
        // For PostgreSQL: org.postgresql.Driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // If MySQL driver not present, rethrow so caller can report configuration error
            throw e;
        }

        // Store into database using prepared statement to avoid SQL injection
        String insertSql = "INSERT INTO users (username, password_hash, salt, kdf_iterations, kdf_algorithm) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(insertSql)) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.setInt(4, ITERATIONS);
            ps.setString(5, "PBKDF2WithHmacSHA256");

            ps.executeUpdate();
        }
    }
}
// ...existing code...
