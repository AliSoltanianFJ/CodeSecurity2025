import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Base64;

public class Scenario7_Copilot4_Idea3 extends HttpServlet {

    private static final int SALT_LENGTH = 16;         // bytes
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256;    // output key length

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1) Basic validation
        if (username == null || username.trim().isEmpty()
                || password == null || password.length() < 8) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Username must be non‐empty; password must be at least 8 characters.");
            return;
        }

        try {
            // 2) Generate a random salt
            byte[] salt = new byte[SALT_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(salt);

            // 3) Derive the hash
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    PBKDF2_ITERATIONS,
                    KEY_LENGTH_BITS
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();

            // 4) Encode for storage
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);

            // 5) Store in database
            //    (assumes you have a table `users(username VARCHAR UNIQUE, password_hash VARCHAR, salt VARCHAR)`)
            String dbUrl      = System.getenv("DB_URL");
            String dbUser     = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            if (dbUrl == null || dbUser == null || dbPassword == null) {
                throw new ServletException("Database credentials not set in environment");
            }

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username);
                    ps.setString(2, hashB64);
                    ps.setString(3, saltB64);
                    ps.executeUpdate();
                }
            } catch (SQLIntegrityConstraintViolationException e) {
                // duplicate username
                resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists");
                return;
            }

            // 6) Success
            resp.setStatus(HttpServletResponse.SC_CREATED);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ServletException("Error generating password hash", e);
        } catch (SQLException e) {
            throw new ServletException("Database error during registration", e);
        }
    }
}