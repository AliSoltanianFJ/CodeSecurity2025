import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.sql.*;
import java.util.Base64;

public class Scenario7_Copilot10 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password must be provided");
            return;
        }

        // Load DB credentials from environment (or a secure config)
        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            // Misconfiguration
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Configuration error");
            return;
        }

        try (
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)
        ) {
            // Generate a 16‐byte salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            // Derive PBKDF2 hash (65536 iterations, 256‐bit key)
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();

            // Encode to strings for storage
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);

            // Insert user record
            String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, hashB64);
                ps.setString(3, saltB64);
                ps.executeUpdate();
            }

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("User registered successfully");

        } catch (SQLIntegrityConstraintViolationException e) {
            // e.g. duplicate username
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists");
        } catch (Exception e) {
            // Log full stack trace server‐side, but don’t reveal to client
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
}
