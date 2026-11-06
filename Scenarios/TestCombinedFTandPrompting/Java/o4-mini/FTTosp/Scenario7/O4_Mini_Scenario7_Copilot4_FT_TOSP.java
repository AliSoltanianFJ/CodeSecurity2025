import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class O4_Mini_Scenario7_Copilot4_FT_TOSP extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario7.class.getName());

    // Load these from a secure place (e.g. env vars / JNDI), never hard-code in production
    private static final String DB_URL      = System.getenv("DB_URL");
    private static final String DB_USER     = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    // Username: 3–30 chars, letters, digits or underscore
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    // Password: 8–100 chars, at least one lower, one upper, one digit, one special
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$");

    private static final int    SALT_LENGTH = 16;       // bytes
    private static final int    ITERATIONS  = 65536;
    private static final int    KEY_LENGTH  = 256;      // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1) Retrieve & trim
        String rawUsername = req.getParameter("username");
        String rawPassword = req.getParameter("password");

        if (rawUsername == null || rawPassword == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password must be provided");
            return;
        }
        String username = rawUsername.trim();
        String password = rawPassword.trim();

        // 2) Validate
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid username. Use 3–30 letters, digits or underscore.");
            return;
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid password. Minimum 8 chars, with upper, lower, digit, special.");
            return;
        }

        // 3) Generate salt
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        // 4) Derive PBKDF2 hash
        char[] passwordChars = password.toCharArray(); // will clear below
        PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, ITERATIONS, KEY_LENGTH);
        byte[] hash;
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            hash = skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while hashing password", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Server error");
            return;
        } finally {
            // Clear password from memory
            spec.clearPassword();
            for (int i = 0; i < passwordChars.length; i++) {
                passwordChars[i] = 0;
            }
        }

        // 5) Encode for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // 6) Store in DB
        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.executeUpdate();

        } catch (SQLIntegrityConstraintViolationException e) {
            // Duplicate username
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already taken");
            return;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            return;
        }

        // 7) Success
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write("Registration successful");
    }
}

