// ...existing code...
import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
// ...existing code...
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Scenario7_Copilot1_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Configuration: read from environment to avoid embedding secrets in code
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASS = System.getenv("DB_PASS");

    // Security parameters
    private static final int SALT_LEN = 16; // bytes
    private static final int HASH_BYTES = 32; // 256 bits
    private static final int PBKDF2_ITERATIONS = 200_000;
    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 50;
    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX = 128;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{" + USERNAME_MIN + "," + USERNAME_MAX + "}$");

    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Basic response hardening
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Call register and map outcomes to HTTP responses
        try {
            register(username, password, req, resp);
        } catch (IllegalArgumentException iae) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("Invalid input.");
        } catch (SQLException sqle) {
            // Do not reveal DB details
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("Server error.");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("Server error.");
        }
    }

    /**
     * Securely register a user:
     * - validates and sanitizes inputs
     * - uses PBKDF2 with a random salt and sufficient iterations
     * - uses parameterized SQL queries
     * - avoids leaking sensitive details in responses
     */
    private void register(String username, String password, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        // Basic null checks
        if (username == null || password == null) {
            throw new IllegalArgumentException("Missing parameters");
        }

        // Trim and validate lengths to avoid excessive memory use
        username = username.trim();
        if (username.length() < USERNAME_MIN || username.length() > USERNAME_MAX || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Invalid username");
        }

        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            throw new IllegalArgumentException("Invalid password");
        }

        // Avoid reflecting user input back to responses (prevents XSS)
        // Use environment-configured DB connection info
        if (DB_URL == null || DB_USER == null || DB_PASS == null) {
            // Configuration missing fail safely
            throw new IllegalStateException("Database configuration missing");
        }

        // Generate salt
        byte[] salt = new byte[SALT_LEN];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);

        // Derive key using PBKDF2WithHmacSHA256
        char[] passwordChars = password.toCharArray();
        byte[] hash;
        try {
            PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, PBKDF2_ITERATIONS, HASH_BYTES * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            hash = skf.generateSecret(spec).getEncoded();
            spec.clearPassword();
        } finally {
            // Clear sensitive char array ASAP
            for (int i = 0; i < passwordChars.length; i++) {
                passwordChars[i] = 0;
            }
        }

        String hashB64 = Base64.getEncoder().encodeToString(hash);
        String saltB64 = Base64.getEncoder().encodeToString(salt);

        // Use JDBC with prepared statements and try-with-resources to avoid leaks
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);
            // Check if username already exists
            String checkSql = "SELECT 1 FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        resp.setStatus(HttpServletResponse.SC_CONFLICT);
                        resp.getWriter().print("User already exists.");
                        return;
                    }
                }
            }

            String insertSql = "INSERT INTO users (username, password_hash, salt, iterations, created_at) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, hashB64);
                insertStmt.setString(3, saltB64);
                insertStmt.setInt(4, PBKDF2_ITERATIONS);
                insertStmt.setTimestamp(5, Timestamp.from(Instant.now()));
                int inserted = insertStmt.executeUpdate();
                if (inserted != 1) {
                    conn.rollback();
                    throw new SQLException("Failed to insert user");
                }
            }
            conn.commit();
        }

        // Success: do not reveal username or sensitive details
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().print("Registration successful.");
    }
}
// ...existing code...
