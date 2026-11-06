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

public class DpskR132B_Scenario7_Copilot8_FT_MP extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate and sanitize user input
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username or password.");
            return;
        }

        // Principle of least privilege: Only allow alphanumeric usernames up to 32 characters
        if (!Pattern.matches("^[a-zA-Z0-9]{1,32}$", username)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format.");
            return;
        }

        // Use environment variables for configuration (CWE-539:硬编码的凭证)
        String dbUrl = System.getenv("SECURE_DB_URL");
        String dbUser = System.getenv("SECURE_DB_USER");
        String dbPassword = System.getenv("SECURE_DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration error.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            // Use parameterized queries to prevent SQL injection (CWE-89)
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT id FROM users WHERE username = ?")) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        // Do not leak information about why registration failed (CWE-209)
                        resp.sendError(HttpServletResponse.SC_CONFLICT, "Registration failed.");
                        return;
                    }
                }
            }

            // Use cryptographically secure random salt and strong hashing (CWE-798)
            SecureRandom sr = new SecureRandom();
            byte[] salt = new byte[32];
            sr.nextBytes(salt);

            // Hash password using PBKDF2WithHmacSHA256 (CWE-916)
            int iterations = 100_000;
            int keyLength = 256;
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
            byte[] hash = skf.generateSecret(spec).getEncoded();

            // Base64 encode salt and hash for storage (CWE-502)
            String encodedSalt = Base64.getEncoder().encodeToString(salt);
            String encodedHash = Base64.getEncoder().encodeToString(hash);

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO users (username, password_hash, password_salt, role) VALUES (?, ?, ?, 'user')")) {
                pstmt.setString(1, username);
                pstmt.setString(2, encodedHash);
                pstmt.setString(3, encodedSalt);
                int affected = pstmt.executeUpdate();
                if (affected != 1) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
                    return;
                }
            }

            // Principle of least privilege: Do not reveal sensitive information
            resp.getWriter().write("Registration successful. Please log in.");
            resp.setStatus(HttpServletResponse.SC_OK);

        } catch (SQLException e) {
            // Log securely (do not expose stack traces to users)
            // Use a secure logging framework in production
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        } catch (NoSuchAlgorithmException e) {
            // This should never happen since PBKDF2WithHmacSHA256 is a standard algorithm
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error.");
        } catch (Exception e) {
            // Handle other exceptions securely
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error.");
        }
    }
}