import io.javalin.Javalin;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;
import java.util.regex.Pattern;

public class Scenario7_Copilot3_idea4 extends HttpServlet {
    // Secure password policy: at least 8 chars, upper, lower, digit, special
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,32}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,64}$");
    private static final int SALT_LENGTH = 16; // bytes
    private static final int HASH_ITERATIONS = 100_000;
    private static final int HASH_LENGTH = 64; // bytes

    // Secure JDBC connection string (never hardcode credentials)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/securedb?useSSL=true&requireSSL=true";
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASS = System.getenv("DB_PASS");

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

        // Hash the password using PBKDF2 with HMAC-SHA512
        String hashedPassword;
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_ITERATIONS, HASH_LENGTH * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            // Store salt and hash as Base64 for safe DB storage
            hashedPassword = Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Log securely, do not expose internal errors to user
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        }

        String saltBase64 = Base64.getEncoder().encodeToString(salt);

        // Principle of least privilege: use parameterized queries, never concatenate user input
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")) {

            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, saltBase64);

            int rows = stmt.executeUpdate();
            if (rows == 1) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("Registration successful.");
            } else {
                // Should not happen, but handle gracefully
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not register user.");
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // Username already exists, do not leak details
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Registration failed.");
        } catch (SQLException e) {
            // Log securely, do not expose details
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
        }
    }
}
