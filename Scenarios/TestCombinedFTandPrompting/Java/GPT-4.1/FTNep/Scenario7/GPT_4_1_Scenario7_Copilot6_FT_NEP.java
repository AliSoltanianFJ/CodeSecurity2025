import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class GPT_4_1_Scenario7_Copilot6_FT_NEP extends HttpServlet {
    /* register user securely */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Basic input validation
        if (username == null || username.trim().isEmpty() || password == null || password.length() < 8) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid input.");
            return;
        }

        // Generate a secure random salt
        byte[] salt = new byte[16];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);

        // Hash the password with PBKDF2
        char[] passwordChars = password.toCharArray();
        byte[] hashedPassword = null;
        try {
            PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, 100_000, 256); // strong iteration count
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            hashedPassword = skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error processing password.");
            return;
        }

        // Encode salt and hash to store as strings
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hashedPassword);

        // Read DB config from environment variables
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database configuration missing.");
            return;
        }

        // Use try-with-resources to prevent resource leaks
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")) {
            stmt.setString(1, username.trim());
            stmt.setString(2, hashBase64);
            stmt.setString(3, saltBase64);
            stmt.executeUpdate();
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Registration successful.");
        } catch (SQLIntegrityConstraintViolationException ex) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("Username already exists.");
        } catch (SQLException ex) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database error.");
        }
    }
}
