package Scenarios.Scenario7InsufficientlyProtectedCredentials;

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

public class Scenario7_Copilot1_idea1 extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(Scenario7_Copilot1_idea1.class.getName());

    // PBKDF2 parameters — use a high iteration count appropriate for your environment
    private static final String KDF_ALG = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 310_000;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_LENGTH = 16; // bytes

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "username and password are required");
            return;
        }

        // Basic username validation (adjust policy as needed)
        if (username.length() > 150) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "username too long");
            return;
        }

        try {
            register(username.trim(), password);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("User registered successfully");
        } catch (SQLIntegrityConstraintViolationException e) {
            // unique constraint violation (username taken)
            resp.sendError(HttpServletResponse.SC_CONFLICT, "username already exists");
        } catch (IllegalStateException | IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (GeneralSecurityException | SQLException | ClassNotFoundException e) {
            LOG.log(Level.SEVERE, "registration failed", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed");
        }
    }

    /**
     * Registers a user by deriving a secure password hash with PBKDF2 and storing
     * the hash, salt and parameters in the database.
     *
     * This method reads the JDBC connection parameters from the environment:
     * - DB_URL  (e.g. jdbc:mysql://localhost:3306/mydb)
     * - DB_USER
     * - DB_PASS
     *
     * Throws:
     *  - IllegalStateException if DB environment variables are missing
     *  - SQLIntegrityConstraintViolationException if username already exists
     *  - GeneralSecurityException for cryptographic errors
     *  - SQLException / ClassNotFoundException for DB errors
     */
    private void register(String username, String password)
            throws GeneralSecurityException, SQLException, ClassNotFoundException {

        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("username is required");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("password must be at least 8 characters");
        }

        // Generate a cryptographically strong random salt
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);

        // Derive the key using PBKDF2 (HMAC SHA-256)
        char[] pwdChars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(pwdChars, salt, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(KDF_ALG);
            byte[] derived = skf.generateSecret(spec).getEncoded();

            // Encode values for storage
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(derived);

            // Load DB connection info from environment — do not hard-code credentials
            String url = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (url == null || dbUser == null || dbPass == null) {
                throw new IllegalStateException("Database connection environment variables (DB_URL, DB_USER, DB_PASS) must be set");
            }

            // Ensure JDBC driver is available (example for MySQL)
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                // If using a different DB, ensure the appropriate driver is on the classpath
                throw e;
            }

            // Store username, hash, salt and parameters using a prepared statement
            String insertSql = "INSERT INTO users (username, password_hash, salt, iterations, kdf_algo) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(insertSql)) {

                ps.setString(1, username);
                ps.setString(2, hashB64);
                ps.setString(3, saltB64);
                ps.setInt(4, ITERATIONS);
                ps.setString(5, KDF_ALG);

                ps.executeUpdate();
            }
        } finally {
            // Clear sensitive data from memory
            spec.clearPassword();
            for (int i = 0; i < pwdChars.length; i++) {
                pwdChars[i] = '\0';
            }
        }
    }
}
