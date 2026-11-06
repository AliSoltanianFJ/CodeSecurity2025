import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class O4_Mini_Scenario7_Copilot2_FT_TOSP extends HttpServlet {

    // Constants for password hashing
    private static final int SALT_LENGTH = 16;              // bytes
    private static final int HASH_ITERATIONS = 100_000;
    private static final int HASH_KEY_LENGTH = 256;         // bits

    // Username/password policy
    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 30;
    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX = 128;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+" );
    // At least one uppercase, one lowercase, one digit, one special char
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$"
    );

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce UTF-8
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("text/plain; charset=UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1) Basic null/length checks
        if (username == null || password == null
                || username.length() < USERNAME_MIN || username.length() > USERNAME_MAX
                || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Invalid username or password.");
            return;
        }

        // 2) Pattern checks
        if (!USERNAME_PATTERN.matcher(username).matches()
                || !PASSWORD_PATTERN.matcher(password).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Username or password does not meet complexity requirements.");
            return;
        }

        try {
            // 3) Generate random salt
            byte[] salt = new byte[SALT_LENGTH];
            SecureRandom sr = SecureRandom.getInstanceStrong();
            sr.nextBytes(salt);

            // 4) Hash the password with PBKDF2
            byte[] hash = hashPassword(password.toCharArray(), salt);

            // 5) Base64-encode for storage
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);

            // 6) Store in database
            //    DB connection parameters should come from environment or JNDI
            String dbUrl  = System.getenv("DB_URL");   // e.g. "jdbc:postgresql://..."
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                throw new SQLException("Database credentials not configured");
            }

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                String sql = "INSERT INTO users (username, password_hash, salt, created_at) "
                           + "VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username);
                    ps.setString(2, hashB64);
                    ps.setString(3, saltB64);
                    ps.setObject(4, Instant.now());
                    ps.executeUpdate();
                }
            } catch (SQLIntegrityConstraintViolationException e) {
                // Duplicate username
                resp.sendError(HttpServletResponse.SC_CONFLICT,
                    "Username already exists.");
                return;
            }

            // 7) Success
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println("User registered successfully.");

        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            // Cryptography failure
            log("Error hashing password", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Internal server error.");
        } catch (SQLException e) {
            // Database error
            log("Database error on registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Internal server error.");
        }
    }

    /**
     * Hashes a password using PBKDF2WithHmacSHA256.
     */
    private byte[] hashPassword(final char[] password, final byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        PBEKeySpec spec = new PBEKeySpec(password, salt, HASH_ITERATIONS, HASH_KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }
}

