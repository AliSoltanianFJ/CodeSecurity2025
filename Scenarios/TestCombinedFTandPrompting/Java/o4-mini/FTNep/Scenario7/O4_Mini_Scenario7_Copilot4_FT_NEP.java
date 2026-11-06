import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class O4_Mini_Scenario7_Copilot4_FT_NEP extends HttpServlet {

    // read from environment
    private static final String DB_URL      = System.getenv("DB_URL");
    private static final String DB_USER     = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    // simple alphanumeric + _ . - @ check, 3–30 chars
    private static final Pattern USERNAME_RE = Pattern.compile("^[A-Za-z0-9_@\\.-]{3,30}$");

    // PBKDF2 parameters
    private static final String  HASH_ALGO   = "PBKDF2WithHmacSHA256";
    private static final int     SALT_LEN    = 16;      // bytes
    private static final int     ITERATIONS  = 100_000;
    private static final int     KEY_LENGTH  = 256;     // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/plain; charset=UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1. Basic null/empty checks
        if (username == null || password == null
                || username.isEmpty() || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password are required.");
            return;
        }

        // 2. Validate username format
        if (!USERNAME_RE.matcher(username).matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid username format.");
            return;
        }

        // 3. Hash the password
        String passwordHash;
        try {
            passwordHash = hashPassword(password);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log("Error hashing password", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error.");
            return;
        }

        // 4. Store in DB
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("User registered successfully.");

        } catch (SQLIntegrityConstraintViolationException e) {
            // e.g. duplicate username
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("Username already exists.");
        } catch (SQLException e) {
            log("Database error during registration", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error.");
        }
    }

    /**
     * Hashes the given password with PBKDF2 + HMAC-SHA256 and a random salt.
     * Returns a string in the form Base64(salt) + ":" + Base64(hash).
     */
    private String hashPassword(String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        // 1) generate a random salt
        SecureRandom rng = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[SALT_LEN];
        rng.nextBytes(salt);

        // 2) derive the key
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(HASH_ALGO);
        byte[] hash = skf.generateSecret(spec).getEncoded();

        // 3) return salt:hash Base64-encoded
        String b64Salt = Base64.getEncoder().encodeToString(salt);
        String b64Hash = Base64.getEncoder().encodeToString(hash);
        return b64Salt + ":" + b64Hash;
    }
}

