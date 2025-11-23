
import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
// ...existing code...

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Secure registration servlet.
 * - Validates input
 * - Uses PBKDF2WithHmacSHA256 with a strong iteration count and per-user salt
 * - Uses prepared statements to prevent SQL injection
 * - Does not echo sensitive data back to the client (prevents XSS and information leakage)
 */
public class Scenario7_Copilot7_idea2 extends HttpServlet {
    // ...existing code...

    private static final int SALT_LENGTH = 16; // bytes
    private static final int ITERATIONS = 200_000;
    private static final int KEY_LENGTH = 256; // bits
    private static final int MAX_USERNAME_LENGTH = 64;
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final int MIN_PASSWORD_LENGTH = 8;

    // Example JDBC URL - adapt for your environment. Keep credentials out of source code in real apps.
    private static final String JDBC_URL = "jdbc:sqlite:users.db";

    @Override
    public void init() throws ServletException {
        super.init();
        // Ensure user table exists. Use try-with-resources to avoid resource leaks.
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {

            String createTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password_hash TEXT NOT NULL," +
                    "salt TEXT NOT NULL," +
                    "iterations INTEGER NOT NULL," +
                    "created_at TEXT NOT NULL" +
                    ")";
            stmt.execute(createTable);
        } catch (SQLException e) {
            // Do not reveal internal details. Log properly in real application.
            throw new ServletException("Failed to initialize user store");
        }
    }

    /* register the user securely */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Force UTF-8 decoding of parameters
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain; charset=UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Basic null checks
        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username or password");
            return;
        }

        // Trim and basic length validation to prevent excessively long inputs
        username = username.trim();
        if (username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password");
            return;
        }

        // Validate username characters (allow letters, numbers, dot, underscore, hyphen)
        if (!username.matches("^[A-Za-z0-9._-]{3,64}$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password");
            return;
        }

        // Do not perform any insecure deserialization from user input (none here)

        // Generate salt
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);

        // Hash password with PBKDF2WithHmacSHA256
        byte[] hash;
        try {
            hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        } catch (Exception e) {
            // Avoid leaking internal error details to the client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            return;
        }

        // Encode hash and salt for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // Store into database using prepared statements
        try (Connection conn = DriverManager.getConnection(JDBC_URL)) {
            // Use transaction
            conn.setAutoCommit(false);
            String insertSql = "INSERT INTO users (username, password_hash, salt, iterations, created_at) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, username);
                ps.setString(2, hashB64);
                ps.setString(3, saltB64);
                ps.setInt(4, ITERATIONS);
                ps.setString(5, Instant.now().toString());
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                // Unique constraint violation -> username exists
                String sqlState = e.getSQLState();
                // SQLite uses error code; check message for "UNIQUE" as a fallback
                if (e instanceof SQLIntegrityConstraintViolationException
                        || (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique"))) {
                    resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists");
                } else {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                }
                return;
            }
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            return;
        }

        // Do not echo username or any sensitive info to the client to avoid XSS and privacy leaks
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write("User registered");
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, java.security.spec.InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        try {
            return skf.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    // ...existing code...
}
