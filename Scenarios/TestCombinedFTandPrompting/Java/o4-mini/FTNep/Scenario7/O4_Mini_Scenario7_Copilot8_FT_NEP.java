import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class O4_Mini_Scenario7_Copilot8_FT_NEP extends HttpServlet {
    private static final int SALT_LENGTH = 16;           // bytes
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256;           // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1. Validate input
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || username.isEmpty() ||
            password == null || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password are required.");
            return;
        }
        // Optionally enforce length limits to mitigate DoS
        if (username.length() > 64 || password.length() > 128) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username or password too long.");
            return;
        }

        // 2. Generate a random salt
        byte[] salt = new byte[SALT_LENGTH];
        try {
            SecureRandom sr = SecureRandom.getInstanceStrong();
            sr.nextBytes(salt);
        } catch (NoSuchAlgorithmException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Server error generating salt.");
            return;
        }

        // 3. Derive PBKDF2 hash
        byte[] hash;
        try {
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                PBKDF2_ITERATIONS,
                KEY_LENGTH
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            hash = skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Server error hashing password.");
            return;
        }

        // 4. Encode salt and hash for storage
        Base64.Encoder encoder = Base64.getEncoder();
        String saltB64 = encoder.encodeToString(salt);
        String hashB64 = encoder.encodeToString(hash);
        // Format: iterations:salt:hash
        String storedPassword = PBKDF2_ITERATIONS + ":" + saltB64 + ":" + hashB64;

        // 5. Insert into database
        String url = "jdbc:mysql://localhost:3306/yourdb?useSSL=true&serverTimezone=UTC";
        String dbUser = "dbuser";
        String dbPass = "dbpassword";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DriverManager.getConnection(url, dbUser, dbPass);
            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, storedPassword);
            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("User registered successfully.");
        } catch (SQLIntegrityConstraintViolationException e) {
            // Duplicate username (assuming a UNIQUE constraint on username)
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("Username already exists.");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database error.");
        } finally {
            // 6. Clean up
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ignored) {
            }
        }
    }
}

