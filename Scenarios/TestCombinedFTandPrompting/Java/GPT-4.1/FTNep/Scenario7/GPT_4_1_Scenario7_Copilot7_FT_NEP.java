import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.regex.Pattern;


/**
 * Secure password registration: 
 * - Uses PBKDF2 with a random salt for password hashing
 * - Validates and sanitizes all user input
 * - Uses prepared statements to prevent SQL injection
 * - Handles errors securely without leaking sensitive info
 * - Assumes database credentials are stored securely outside source code
 */
public class GPT_4_1_Scenario7_Copilot7_FT_NEP extends HttpServlet {
    // Strong username and password patterns
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,32}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,64}$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce HTTPS (should be done in server config, but double-check here)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input
        if (username == null || password == null ||
            !USERNAME_PATTERN.matcher(username).matches() ||
            !PASSWORD_PATTERN.matcher(password).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password format.");
            return;
        }

        // Hash password securely
        String salt = generateSalt();
        String passwordHash;
        try {
            passwordHash = hashPassword(password, salt);
        } catch (NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        }

        // Store credentials securely using prepared statements
        try (Connection conn = DatabaseUtils.getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")) {
            stmt.setString(1, username.trim());
            stmt.setString(2, passwordHash);
            stmt.setString(3, salt);
            stmt.executeUpdate();
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Registration successful.");
        } catch (SQLException ex) {
            // Log securely (never disclose details to client)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
        }
    }

    // Secure salt generator
    private String generateSalt() {
        byte[] salt = new byte[32];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // PBKDF2 password hasher
    private String hashPassword(String password, String salt)
            throws NoSuchAlgorithmException, java.security.spec.InvalidKeySpecException {
        char[] pwdChars = password.toCharArray();
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        PBEKeySpec spec = new PBEKeySpec(pwdChars, saltBytes, 100_000, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }
}

// Secure DB connection utility (credentials/config should be in environment variables or secure vault)
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseUtils {
    public static Connection getConnection() throws SQLException {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String pass = System.getenv("DB_PASS");
        if (url == null || user == null || pass == null) {
            throw new SQLException("Database credentials not set in environment variables.");
        }
        return DriverManager.getConnection(url, user, pass);
    }
}
