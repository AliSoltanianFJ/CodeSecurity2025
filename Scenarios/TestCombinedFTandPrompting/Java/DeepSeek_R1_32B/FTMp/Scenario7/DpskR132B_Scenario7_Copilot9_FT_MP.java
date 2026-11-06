import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

public class DpskR132B_Scenario7_Copilot9_FT_MP extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Validate and sanitize user input
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Secure input validation
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        // Secure input validation: username length (3-32 chars)
        if (username.length() < 3 || username.length() > 32) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username must be between 3 and 32 characters.");
            return;
        }

        // Secure input validation: password complexity (at least 8 chars, upper, lower, digit, special)
        if (password.length() < 8 || !Pattern.compile("[A-Z]").matcher(password).find() ||
                !Pattern.compile("[a-z]").matcher(password).find() ||
                !Pattern.compile("[0-9]").matcher(password).find() ||
                !Pattern.compile("[^A-Za-z0-9]").matcher(password).find()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password must be at least 8 characters and contain at least one uppercase letter, one lowercase letter, one digit, and one special character.");
            return;
        }

        // Principle of least privilege: only allow alphanumeric and underscore in username
        if (!Pattern.compile("^[A-Za-z0-9_]+$").matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username can only contain alphanumeric characters and underscores.");
            return;
        }

        // Secure password hashing using PBKDF2WithHmacSHA256
        try {
            // Generate a cryptographically secure random salt
            SecureRandom sr = SecureRandom.getInstanceStrong();
            byte[] salt = new byte[32];
            sr.nextBytes(salt);
            String saltBase64 = Base64.getEncoder().encodeToString(salt);

            // Hash the password with PBKDF2
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 100000, 256);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            String hashBase64 = Base64.getEncoder().encodeToString(hash);

            // Secure database connection (use environment variables for credentials)
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            if (dbUrl == null || dbUser == null || dbPassword == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration error.");
                return;
            }

            // Use parameterized queries to prevent SQL injection
            String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, username);
                pstmt.setString(2, hashBase64);
                pstmt.setString(3, saltBase64);
                pstmt.executeUpdate();

                resp.getWriter().write("Registration successful!");

            } catch (SQLException e) {
                // Secure error handling: do not leak sensitive info
                System.err.println("Error registering user: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to register user.");
            }

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Secure error handling: log internally, return generic error
            System.err.println("Error hashing password: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Password hashing failed.");
        }
    }
}