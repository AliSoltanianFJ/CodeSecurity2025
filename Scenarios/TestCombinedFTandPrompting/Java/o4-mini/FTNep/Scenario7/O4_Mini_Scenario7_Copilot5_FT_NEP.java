import java.io.IOException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class O4_Mini_Scenario7_Copilot5_FT_NEP extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // You should set these in your environment or configuration
    private static final String DB_URL      = System.getenv("DB_URL");
    private static final String DB_USER     = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    // PBKDF2 parameters
    private static final int SALT_LENGTH   = 16;       // bytes
    private static final int HASH_ITER     = 100_000;  // iterations
    private static final int KEY_LENGTH    = 256;      // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1) Basic null/empty checks
        if (username == null || password == null
                || username.isEmpty()  || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password are required.");
            return;
        }

        // 2) Validate username (alphanumeric + underscore, 3–20 chars)
        if (!username.matches("^[A-Za-z0-9_]{3,20}$")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username must be 3–20 characters, letters/numbers/underscore only.");
            return;
        }

        // 3) Enforce a minimum password length
        if (password.length() < 8) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Password must be at least 8 characters.");
            return;
        }

        // 4) Check for existing username
        try {
            if (isUsernameTaken(username)) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("Username already exists.");
                return;
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database error checking username: " + e.getMessage());
            return;
        }

        // 5) Generate salt + hash
        byte[] salt = generateSalt();
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64;
        try {
            hashB64 = hashPassword(password, salt);
        } catch (Exception e) {
            throw new ServletException("Error hashing password", e);
        }

        // 6) Insert new user
        String insertSql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(insertSql)) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);

            int updated = ps.executeUpdate();
            if (updated == 1) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("User registered successfully.");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Registration failed.");
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database error during registration: " + e.getMessage());
        }
    }

    /**
     * Check if the username is already present in the database.
     */
    private boolean isUsernameTaken(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Generate a cryptographically strong random salt.
     */
    private byte[] generateSalt() {
        SecureRandom rnd = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        rnd.nextBytes(salt);
        return salt;
    }

    /**
     * Hash the given password with the given salt using PBKDF2WithHmacSHA256.
     * Returns the Base64‐encoded hash.
     */
    private String hashPassword(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            HASH_ITER,
            KEY_LENGTH
        );
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }
}

