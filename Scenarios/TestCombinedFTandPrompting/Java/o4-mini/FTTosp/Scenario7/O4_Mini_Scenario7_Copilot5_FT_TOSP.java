import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class O4_Mini_Scenario7_Copilot5_FT_TOSP extends HttpServlet {
    private static final int SALT_LENGTH = 16;               // bytes
    private static final int HASH_ITERATIONS = 65536;
    private static final int HASH_KEY_LENGTH = 256;          // bits
    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 30;
    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX = 128;
    // Adjust these as needed, or pull from a secure config
    private static final String DB_URL      = System.getenv("DB_URL");
    private static final String DB_USER     = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce UTF-8 encoding on input and output
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain; charset=UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1) Basic null/empty checks
        if (username == null || password == null
                || username.trim().isEmpty() || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Username and password are required.");
            return;
        }
        username = username.trim();

        // 2) Enforce length constraints
        if (username.length() < USERNAME_MIN || username.length() > USERNAME_MAX
                || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Invalid username or password length.");
            return;
        }

        // 3) Whitelist username characters (alphanumeric, underscore, hyphen, dot)
        if (!username.matches("^[A-Za-z0-9_.-]+$")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Username contains invalid characters.");
            return;
        }

        try {
            // 4) Generate a per-user random salt
            byte[] salt = new byte[SALT_LENGTH];
            SecureRandom rng = SecureRandom.getInstanceStrong();
            rng.nextBytes(salt);

            // 5) Hash the password with PBKDF2
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    HASH_ITERATIONS,
                    HASH_KEY_LENGTH
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();

            // 6) Encode to Base64 for safe storage
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);

            // 7) Store username, hash, and salt in the database
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)"
                 )) {
                ps.setString(1, username);
                ps.setString(2, hashB64);
                ps.setString(3, saltB64);
                ps.executeUpdate();
            } catch (SQLIntegrityConstraintViolationException dup) {
                // Duplicate username
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().println("That username is already taken.");
                return;
            }

            // 8) Success
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println("User registered successfully.");

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Crypto algorithm not available
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Internal error; please try again later.");
            // In a real system, log e at WARN/ERROR level without exposing details to the client
        } catch (SQLException e) {
            // Other database errors
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Internal error; please try again later.");
            // Log e safely
        }
    }
}

