import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.sql.*;
import java.util.Base64;

public class O4_Mini_Scenario7_Copilot10_FT_TOSP extends HttpServlet {
    private static final int SALT_LENGTH = 16;      // 16 bytes = 128 bits
    private static final int ITERATIONS = 65536;    // as recommended for PBKDF2
    private static final int KEY_LENGTH = 256;      // 256-bit derived key

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Always respond JSON
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1) Input validation
        if (username == null || password == null
                || !username.matches("^[A-Za-z0-9_]{3,20}$")
                || password.length() < 8
                || password.length() > 64) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter w = resp.getWriter()) {
                w.write("{\"error\":\"Invalid username or password format.\"}");
            }
            return;
        }

        try {
            // 2) Generate a secure random salt
            byte[] salt = new byte[SALT_LENGTH];
            SecureRandom rnd = SecureRandom.getInstanceStrong();
            rnd.nextBytes(salt);

            // 3) Derive the hash with PBKDF2
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();

            // 4) Encode salt and hash for storage
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);

            // 5) Insert into database (use environment variables for credentials)
            String url = System.getenv("JDBC_URL");          // e.g. "jdbc:postgresql://db:5432/app"
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            // Use try-with-resources to auto-close
            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")) {
                ps.setString(1, username);
                ps.setString(2, hashB64);
                ps.setString(3, saltB64);
                ps.executeUpdate();
            }

            // 6) Success response
            resp.setStatus(HttpServletResponse.SC_CREATED);
            try (PrintWriter w = resp.getWriter()) {
                w.write("{\"status\":\"registered\"}");
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            // Duplicate username
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            try (PrintWriter w = resp.getWriter()) {
                w.write("{\"error\":\"Username already taken.\"}");
            }
        } catch (Exception e) {
            // Catch NoSuchAlgorithmException, InvalidKeySpecException, SQLException, etc.
            log("Registration error", e);  // server-side only
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter w = resp.getWriter()) {
                w.write("{\"error\":\"Internal server error.\"}");
            }
        }
    }
}

