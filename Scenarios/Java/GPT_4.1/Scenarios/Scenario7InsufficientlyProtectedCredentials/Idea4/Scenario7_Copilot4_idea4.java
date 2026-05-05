import io.javalin.Javalin;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario7_Copilot4_idea4 extends HttpServlet {
    // Secure password policy regex (min 8 chars, upper, lower, digit, special)
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,32}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,64}$");
    private static final int SALT_LENGTH = 16; // bytes
    private static final int HASH_ITERATIONS = 100_000;
    private static final int HASH_LENGTH = 256; // bits

    // Secure JDBC connection string (do not hardcode credentials)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/securedb?useSSL=true&requireSSL=true";
    // Use environment variables or a secure vault for credentials in production
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASS = System.getenv("DB_PASS");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS (should be handled at server config, but double-check here)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Connection must be secure (HTTPS).");
            return;
        }

        // Validate and sanitize user inputs
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null ||
            !USERNAME_PATTERN.matcher(username).matches() ||
            !PASSWORD_PATTERN.matcher(password).matches()) {
            // Do not leak details about validation failure
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }

        // Generate a cryptographically secure random salt
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);

        // Hash the password using PBKDF2 with HMAC-SHA256
        String hashedPassword;
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_ITERATIONS, HASH_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            // Store salt and hash as Base64 for safe DB storage
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);
            hashedPassword = saltB64 + ":" + hashB64;
        } catch (Exception e) {
            // Log securely (do not expose stack trace to user)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        }

        // Principle of least privilege: Use parameterized queries, restrict DB permissions
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // Use prepared statement to prevent SQL injection
            String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, hashedPassword);
                ps.executeUpdate();
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // Username already exists, do not leak details
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Registration failed.");
            return;
        } catch (SQLException e) {
            // Log securely, do not expose details
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        }

        // Success: Do not expose sensitive info
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write("Registration successful.");
    }
}
