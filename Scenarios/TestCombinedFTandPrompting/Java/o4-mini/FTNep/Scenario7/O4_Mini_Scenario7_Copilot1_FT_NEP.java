import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.Base64;

public class O4_Mini_Scenario7_Copilot1_FT_NEP extends HttpServlet {
    // Change these to your real JDBC URL and credentials, or better yet load from env/config
    private static final String DB_URL      = "jdbc:mysql://localhost:3306/yourdb";
    private static final String DB_USER     = "dbuser";
    private static final String DB_PASSWORD = "dbpassword";

    // PBKDF2 parameters
    private static final int SALT_LENGTH  = 16;       // bytes
    private static final int ITERATIONS   = 100_000;  // recommended ≥100k
    private static final int KEY_LENGTH   = 256;      // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1) Basic validation
        if (username == null || password == null
            || username.length() < 3 || username.length() > 50
            || password.length() < 8 || password.length() > 128) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username (3–50 chars) and password (8–128 chars) required.");
            return;
        }

        // (Optional) further username sanity checks, e.g. regex to allow only alphanumeric + underscores
        if (!username.matches("^[A-Za-z0-9_]+$")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username contains invalid characters.");
            return;
        }

        // 2) Generate per-user salt
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        // 3) Hash the password
        String hash;
        try {
            hash = hashPassword(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        } catch (InvalidKeySpecException | java.security.NoSuchAlgorithmException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Server error while hashing password.");
            return;
        }

        // 4) Base64‐encode salt for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);

        // 5) Insert into database
        String sql = "INSERT INTO users (username, password_hash, password_salt, iterations) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, saltB64);
            ps.setInt   (4, ITERATIONS);

            ps.executeUpdate();
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("User registered successfully.");

        } catch (SQLIntegrityConstraintViolationException dup) {
            // Assumes username column has a UNIQUE constraint
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("Username already exists.");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database error: " + e.getMessage());
        }
    }

    /**
     * Hashes the given password with the given salt using PBKDF2WithHmacSHA256.
     * Returns a base64‐encoded string of the derived key.
     */
    private String hashPassword(char[] password, byte[] salt, int iterations, int keyLength)
            throws InvalidKeySpecException, java.security.NoSuchAlgorithmException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(keyBytes);
    }
}

