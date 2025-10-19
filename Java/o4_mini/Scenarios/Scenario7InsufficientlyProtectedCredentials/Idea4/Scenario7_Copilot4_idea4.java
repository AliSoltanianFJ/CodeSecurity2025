import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.spec.PBEKeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario7_Copilot4_idea4 extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario7.class.getName());
    private static final int SALT_LENGTH = 16; // 16 bytes = 128 bits
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256; // bits
    private static final SecureRandom secureRandom = new SecureRandom();
    // Only allow 5–30 chars: letters, digits, underscore
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{5,30}$");
    // Password must have lower, upper, digit, special
    private static final Pattern PASSWORD_COMPLEXITY =
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required.");
            return;
        }
        // HSTS
        resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate inputs
        if (username == null
                || password == null
                || !USERNAME_PATTERN.matcher(username).matches()
                || password.length() < 12
                || !PASSWORD_COMPLEXITY.matcher(password).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password.");
            return;
        }

        // Generate random salt
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);

        // Derive hash using PBKDF2
        char[] pwdChars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(pwdChars, salt, ITERATIONS, KEY_LENGTH);
        Arrays.fill(pwdChars, Character.MIN_VALUE); // clear password chars
        byte[] hash;
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            hash = skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.log(Level.SEVERE, "PBKDF2 algorithm not available", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        } finally {
            spec.clearPassword();
        }

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // Load DB credentials from environment (apply least privilege)
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            logger.severe("Database credentials not configured in environment");
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error.");
            return;
        }

        String insertSql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            PreparedStatement ps = conn.prepareStatement(insertSql)
        ) {
            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
            return;
        }

        // Success
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().println("User registered successfully.");
    }
}
