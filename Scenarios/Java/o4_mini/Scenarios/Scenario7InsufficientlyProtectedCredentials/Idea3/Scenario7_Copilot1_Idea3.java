import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario7_Copilot1_Idea3 extends HttpServlet {
    private static final int SALT_LENGTH = 16;            // bytes
    private static final int ITERATIONS  = 100_000;      
    private static final int KEY_LENGTH  = 256;           // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1) Basic validation
        if (username == null || password == null ||
            username.trim().isEmpty() || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "Username and password are required");
            return;
        }
        // only allow letters, digits and underscore, 3–50 chars
        if (!username.matches("[A-Za-z0-9_]{3,50}")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "Username must be 3–50 chars: letters, digits or _");
            return;
        }

        try {
            // 2) Generate salt
            byte[] salt = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(salt);

            // 3) Hash the password
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
            );
            SecretKeyFactory skf =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();

            // 4) Base64‐encode for storage
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);

            // 5) Insert into the database
            registerUser(username, hashB64, saltB64);

            // 6) Respond Created
            resp.setStatus(HttpServletResponse.SC_CREATED);

        } catch (SQLIntegrityConstraintViolationException e) {
            // Unique index violation on username
            resp.sendError(HttpServletResponse.SC_CONFLICT,
                           "Username already exists");
        } catch (Exception e) {
            log("Error in user registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Internal server error");
        }
    }

    private void registerUser(String username,
                              String passwordHash,
                              String salt)
            throws SQLException {

        // Load DB connection info from environment
        String url      = System.getenv("DB_URL");
        String dbUser   = System.getenv("DB_USER");
        String dbPass   = System.getenv("DB_PASSWORD");

        // Use try-with-resources to auto-close
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)"
             )) {

            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, salt);
            ps.executeUpdate();
        }
    }
}