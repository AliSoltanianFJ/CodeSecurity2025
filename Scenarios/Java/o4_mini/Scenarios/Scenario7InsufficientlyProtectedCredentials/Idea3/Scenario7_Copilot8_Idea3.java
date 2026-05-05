import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

public class Scenario7_Copilot8_Idea3 extends HttpServlet {
    // Database connection parameters, e.g. set via environment variables
    private static final String DB_URL      = System.getenv("DB_URL");       // e.g. "jdbc:mysql://localhost:3306/mydb"
    private static final String DB_USER     = System.getenv("DB_USER");      // e.g. "appuser"
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");  // e.g. "s3cr3t"

    // PBKDF2 parameters
    private static final int SALT_LENGTH   = 16;       // bytes
    private static final int ITERATIONS    = 10000;
    private static final int KEY_LENGTH    = 256;      // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. Read & validate parameters
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || username.isEmpty() ||
            password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password must be provided");
            return;
        }

        // 2. Generate a random salt
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);

        // 3. Hash the password with PBKDF2
        String hashB64;
        try {
            hashB64 = hashPassword(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while hashing password");
            return;
        }

        // 4. Insert into database
        //    Users table schema assumed:
        //      CREATE TABLE users (
        //        id INT AUTO_INCREMENT PRIMARY KEY,
        //        username VARCHAR(255) NOT NULL UNIQUE,
        //        password_hash VARCHAR(512) NOT NULL,
        //        salt VARCHAR(64) NOT NULL
        //      );
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            // 4a. Check for existing username
            String checkSql = "SELECT 1 FROM users WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already taken");
                        return;
                    }
                }
            }

            // 4b. Insert new user
            String insertSql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, username);
                ps.setString(2, hashB64);
                ps.setString(3, saltB64);
                ps.executeUpdate();
            }

            // 5. Success
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println("User registered successfully");

        } catch (SQLIntegrityConstraintViolationException dupEx) {
            // In case of a rare race condition on unique constraint
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already taken");
        } catch (SQLException sqlEx) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    /**
     * Hashes a password using PBKDF2 with HMAC-SHA256.
     *
     * @param password   the password characters
     * @param salt       the salt bytes
     * @param iterations iteration count
     * @param keyLength  derived key length in bits
     * @return Base64-encoded hash
     */
    private static String hashPassword(
            final char[] password,
            final byte[] salt,
            final int iterations,
            final int keyLength
    ) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }
}