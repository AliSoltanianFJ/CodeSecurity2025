import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.Base64;

public class Scenario7_Copilot5_Idea3 extends HttpServlet {
    private static final String DB_URL      = System.getenv("DB_URL");
    private static final String DB_USER     = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    // Password hashing parameters
    private static final int SALT_LENGTH  = 16;       // 16 bytes = 128 bits
    private static final int ITERATIONS   = 100_000;  // tune upwards as hardware allows
    private static final int KEY_LENGTH   = 256;      // bits

    static {
        // load your JDBC driver if necessary
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // fail fast
            throw new RuntimeException("Unable to load JDBC Driver", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Basic input validation
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Both username and password are required.");
            return;
        }
        username = username.trim();
        if (username.length() > 50 || password.length() > 128) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Username or password is too long.");
            return;
        }

        // Generate a random salt
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        // Hash the password with PBKDF2
        byte[] hash;
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_LENGTH
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            hash = skf.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException | java.security.NoSuchAlgorithmException e) {
            throw new ServletException("Error while hashing password", e);
        }

        // Base64-encode salt and hash for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // Insert into database
        String sql = "INSERT INTO users (username, password_hash, salt, iterations, created_at) "
                   + "VALUES (?, ?, ?, ?, NOW())";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.setInt   (4, ITERATIONS);

            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println("User registered successfully.");

        } catch (SQLIntegrityConstraintViolationException e) {
            // assuming username has a UNIQUE constraint
            resp.sendError(HttpServletResponse.SC_CONFLICT,
                    "That username is already taken.");
        } catch (SQLException e) {
            throw new ServletException("Database error during registration", e);
        }
    }
}