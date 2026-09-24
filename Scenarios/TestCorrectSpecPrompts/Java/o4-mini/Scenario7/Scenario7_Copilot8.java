import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario7_Copilot8 extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario7_Copilot8.class.getName());

    // PBKDF2 parameters
    private static final int SALT_LENGTH = 16;           // bytes
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;           // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Basic input validation
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        // Generate a random salt
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        // Derive the hash
        byte[] hash;
        try {
            hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        } catch (Exception e) {
            // This should rarely happen—something's wrong with the crypto setup
            logger.log(Level.SEVERE, "Error while hashing password", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error.");
            return;
        }

        // Base64‐encode for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // Retrieve DB credentials from environment
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            logger.severe("Database credentials not set in environment");
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal configuration error.");
            return;
        }

        String sql = "INSERT INTO users (username, password_hash, salt, iterations) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username.trim());
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.setInt(4, ITERATIONS);
            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("User registered successfully.");

        } catch (SQLIntegrityConstraintViolationException dupEx) {
            // e.g. unique username constraint violation
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists.");
        } catch (SQLException sqle) {
            logger.log(Level.SEVERE, "Database error during registration", sqle);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error.");
        }
    }

    /**
     * PBKDF2 with HMAC-SHA256
     */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws Exception {

        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }
}