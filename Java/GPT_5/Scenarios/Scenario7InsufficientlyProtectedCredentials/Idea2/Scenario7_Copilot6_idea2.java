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
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
// ...existing code...

public class Scenario7_Copilot6_idea2 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario7.class.getName());

    // PBKDF2 settings - current recommended minimums (adjust as appropriate)
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int ITERATIONS = 200_000; // use a high iteration count
    private static final int DERIVED_KEY_LENGTH_BITS = 256;

    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Basic request validation and protections
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain; charset=UTF-8");

        String username = safeGetParameter(req, "username");
        String password = safeGetParameter(req, "password");

        // Validate lengths and characters to avoid unexpected input
        if (!isValidUsername(username) || !isValidPassword(password)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter w = resp.getWriter()) {
                w.println("Invalid input.");
            }
            return;
        }

        // Delegate to register process which handles exceptions and responses
        register(username, password, resp);
    }

    // Helper to safely read a parameter (avoids null issues and trims)
    private static String safeGetParameter(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        if (v == null) return "";
        v = v.trim();
        // Limit maximum length immediately to prevent excessive memory usage
        if (v.length() > 1024) return v.substring(0, 1024);
        return v;
    }

    // Username: allow letters, numbers, dot, dash, underscore; 3-64 chars
    private static boolean isValidUsername(String username) {
        if (username == null) return false;
        if (username.length() < 3 || username.length() > 64) return false;
        return username.matches("^[A-Za-z0-9._-]+$");
    }

    // Password policy: 8-128 chars (example); do not impose complexity rules here, just length
    private static boolean isValidPassword(String password) {
        if (password == null) return false;
        int len = password.length();
        return len >= 8 && len <= 128;
    }

    // Register a new user securely: PBKDF2 hashing, prepared statements, safe DB access
    private void register(String username, String password, HttpServletResponse resp) throws IOException {
        // Do not log or return raw password
        // Obtain DB credentials from environment variables to avoid hardcoding secrets
        String dbUrl = System.getenv("APP_DB_URL"); // e.g. "jdbc:postgresql://localhost:5432/app"
        String dbUser = System.getenv("APP_DB_USER");
        String dbPassword = System.getenv("APP_DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            LOGGER.log(Level.SEVERE, "Database credentials not configured in environment");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter w = resp.getWriter()) {
                w.println("Server configuration error.");
            }
            return;
        }

        // Generate secure salt
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);

        // Derive key using PBKDF2 with HMAC-SHA256
        byte[] derivedKey;
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, DERIVED_KEY_LENGTH_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(KDF_ALGORITHM);
            derivedKey = skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException e) {
            LOGGER.log(Level.SEVERE, "KDF failure", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter w = resp.getWriter()) {
                w.println("Server error.");
            }
            return;
        }

        // Encode salt and hash as Base64 for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(derivedKey);

        // Insert the new user record safely using prepared statements
        String insertSql = "INSERT INTO users (username, password_hash, salt, kdf_iterations, kdf_algorithm, created_at) "
                + "VALUES (?, ?, ?, ?, ?, current_timestamp)";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(insertSql)) {

            // Set parameters
            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.setInt(4, ITERATIONS);
            ps.setString(5, KDF_ALGORITHM);

            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
            try (PrintWriter w = resp.getWriter()) {
                w.println("User created.");
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            try (PrintWriter w = resp.getWriter()) {
                w.println("User already exists.");
            }
        } catch (SQLException e) {
            // Generic DB error - do not reveal details to client
            LOGGER.log(Level.SEVERE, "Database error during registration", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter w = resp.getWriter()) {
                w.println("Server error.");
            }
        }
    }
}
// ...existing code...
