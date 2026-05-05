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

public class Scenario7_Copilot2_idea4 extends HttpServlet {
    // Secure password policy regex (min 12 chars, at least one upper, lower, digit, special)
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,32}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$");

    // Secure configuration: never hardcode secrets, use environment variables or secure vaults for DB credentials
    private static final String DB_URL = System.getenv("SECURE_DB_URL");
    private static final String DB_USER = System.getenv("SECURE_DB_USER");
    private static final String DB_PASS = System.getenv("SECURE_DB_PASS");

    // PBKDF2 parameters
    private static final int SALT_LENGTH = 16; // bytes
    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256; // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Validate and sanitize user input
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

        // Hash the password securely using PBKDF2
        String hashedPassword;
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            // Store salt and hash as Base64 for safe DB storage
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);
            hashedPassword = saltB64 + ":" + hashB64;
        } catch (Exception e) {
            // Log securely (never log sensitive data)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        }

        // Principle of least privilege: use a dedicated DB user with minimal rights
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // Use parameterized queries to prevent SQL injection
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
            // Generic error handling
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        }

        // Secure response: do not echo sensitive data
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write("Registration successful.");
    }
}
