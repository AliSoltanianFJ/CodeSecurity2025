import io.javalin.Javalin;              // if you actually use Javalin elsewhere
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class O4_Mini_Scenario7_Copilot2_FT_NEP extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // PBKDF2 parameters
    private static final int SALT_LENGTH = 16;               // bytes
    private static final int ITERATIONS  = 65536;
    private static final int KEY_LENGTH  = 256;              // bits

    // TODO: replace these with your real connection parameters or load from config/env
    private static final String DB_URL      = "jdbc:mysql://localhost:3306/yourdb?useSSL=true&serverTimezone=UTC";
    private static final String DB_USER     = "dbuser";
    private static final String DB_PASSWORD = "dbpassword";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1. Read and validate
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null
                || username.length() < 3 || username.length() > 30
                || password.length() < 8 || password.length() > 100
                || !username.matches("[A-Za-z0-9_\\-]+")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid username or password.");
            return;
        }

        // 2. Generate salt and hash the password
        byte[] salt = generateSalt();
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hash;
        try {
            hash = hashPassword(password.toCharArray(), salt);
        } catch (GeneralSecurityException e) {
            // don't reveal details
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error processing password.");
            return;
        }

        // 3. Store (username, hash, salt) in DB
        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, saltB64);
            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Registration successful.");

        } catch (SQLIntegrityConstraintViolationException e) {
            // e.g. duplicate username
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("Username already taken.");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database error.");
            // log e in server logs, do not expose to user
        }
    }

    /**
     * Generates a cryptographically secure random salt.
     */
    private static byte[] generateSalt() {
        SecureRandom rnd = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        rnd.nextBytes(salt);
        return salt;
    }

    /**
     * Derives a PBKDF2‐with‐HMAC‐SHA256 hash of the given password and salt.
     * Returns the Base64‐encoded result.
     */
    private static String hashPassword(char[] password, byte[] salt)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] key = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(key);
    }
}

