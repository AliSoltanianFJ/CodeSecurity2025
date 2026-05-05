import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Scenario7_Copilot3_idea1 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // PBKDF2 parameters - choose strong defaults
    private static final int ITERATIONS = 200_000;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_LENGTH = 16; // bytes

    // Database configuration: prefer environment variables; fall back to placeholders for compilation
    private static final String DB_URL = System.getenv().getOrDefault("DB_URL",
            "jdbc:mysql://localhost:3306/mydb?useSSL=true&serverTimezone=UTC");
    private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "dbuser");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "dbpass");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "username and password are required");
            return;
        }

        try {
            register(username.trim(), password);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("User registered successfully");
        } catch (SQLIntegrityConstraintViolationException e) {
            // username already exists (unique constraint)
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // cryptographic failure
            log("Password hashing error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed");
        } catch (SQLException e) {
            log("Database error during registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed");
        } catch (Exception e) {
            log("Unexpected error during registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed");
        }
    }

    /**
     * Securely registers a user:
     * - generates a random salt
     * - derives a key using PBKDF2WithHmacSHA256 with a high iteration count
     * - stores base64-encoded hash and salt in the database using a prepared statement
     */
    private void register(String username, String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException, SQLException {

        // generate salt
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);

        // derive key using PBKDF2WithHmacSHA256
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] derived = skf.generateSecret(spec).getEncoded();

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(derived);
        String algorithm = "PBKDF2WithHmacSHA256";

        // store into database using prepared statement
        String sql = "INSERT INTO users (username, password_hash, salt, iterations, algorithm) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.setInt(4, ITERATIONS);
            ps.setString(5, algorithm);
            ps.executeUpdate();
        }
    }
}
