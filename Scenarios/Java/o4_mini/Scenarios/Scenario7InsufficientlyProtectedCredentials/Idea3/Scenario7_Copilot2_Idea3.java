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

public class Scenario7_Copilot2_Idea3 extends HttpServlet {
    private static final String JDBC_URL      = System.getenv("DB_URL");
    private static final String DB_USER       = System.getenv("DB_USER");
    private static final String DB_PASSWORD   = System.getenv("DB_PASSWORD");

    // PBKDF2 parameters
    private static final String PBKDF2_ALGO   = "PBKDF2WithHmacSHA256";
    private static final int    ITERATIONS    = 100_000;
    private static final int    KEY_LENGTH    = 256;      // bits
    private static final int    SALT_LENGTH   = 16;       // bytes

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1) basic presence check
        if (username == null || password == null
                || username.isEmpty()  || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password must be provided");
            return;
        }

        // 2) enforce a simple username policy (alphanumeric + underscore, 3–30 chars)
        if (!username.matches("[A-Za-z0-9_]{3,30}")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username must be 3–30 chars, letters/digits/underscore only");
            return;
        }

        try {
            // 3) generate random salt
            SecureRandom rng = SecureRandom.getInstanceStrong();
            byte[] salt = new byte[SALT_LENGTH];
            rng.nextBytes(salt);

            // 4) derive the hash
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_LENGTH
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGO);
            byte[] hash = skf.generateSecret(spec).getEncoded();

            // 5) Base64-encode salt and hash, store as salt:hash
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);
            String saltedHash = saltB64 + ":" + hashB64;

            // 6) store in database
            //    assumes a table like:
            //      CREATE TABLE users (
            //        id BIGINT AUTO_INCREMENT PRIMARY KEY,
            //        username VARCHAR(255) UNIQUE NOT NULL,
            //        password_hash VARCHAR(512) NOT NULL
            //      );
            try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
                String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username);
                    ps.setString(2, saltedHash);
                    ps.executeUpdate();
                }
            }

            // 7) success
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("User registered successfully");

        } catch (SQLIntegrityConstraintViolationException e) {
            // duplicate key on username
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("Username already exists");
        } catch (InvalidKeySpecException | java.security.NoSuchAlgorithmException e) {
            // PBKDF2 errors
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error hashing password");
            log("Password hashing failure", e);
        } catch (SQLException e) {
            // JDBC errors
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database error");
            log("Database error while registering user", e);
        }
    }
}