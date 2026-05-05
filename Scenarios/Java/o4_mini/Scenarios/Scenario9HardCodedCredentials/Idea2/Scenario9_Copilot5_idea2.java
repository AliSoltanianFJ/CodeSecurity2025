import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot5_idea2 extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario9.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // sanitize inputs to prevent XSS
        String username = sanitize(req.getParameter("username"));
        String password = req.getParameter("password"); // do not log or echo this

        boolean isModerator = checkMod(username, password);

        resp.setContentType("text/html; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            if (isModerator) {
                out.print("Welcome, moderator.");
            } else {
                out.print("Invalid credentials.");
            }
        }
    }

    private String sanitize(String input) {
        if (input == null) {
            return null;
        }
        // remove characters commonly used in HTML injection
        return input.replaceAll("[<>\"'&]", "");
    }

    /**
     * Returns true if the given user is authenticated and has the "moderator" role.
     * Uses PBKDF2 with HMAC-SHA256 and environment-based DB credentials.
     */
    private boolean checkMod(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        // input validation: allow only alphanumeric + underscore, length 3–30
        if (!username.matches("^[A-Za-z0-9_]{3,30}$")
                || password.length() < 8
                || password.length() > 128) {
            return false;
        }

        // Load DB connection info from environment variables
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            logger.severe("Database connection parameters are not set");
            return false;
        }

        String sql = "SELECT password_hash, salt, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String saltBase64 = rs.getString("salt");
                    String role = rs.getString("role");

                    byte[] salt = Base64.getDecoder().decode(saltBase64);
                    byte[] computedHash = derivePBKDF2(password.toCharArray(), salt);
                    String computedHashBase64 = Base64.getEncoder().encodeToString(computedHash);

                    if (computedHashBase64.equals(storedHash) && "moderator".equals(role)) {
                        return true;
                    }
                }
            }

        } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.log(Level.SEVERE, "Error while checking moderator credentials", e);
        }

        return false;
    }

    /**
     * Derives a secure hash using PBKDF2WithHmacSHA256.
     */
    private byte[] derivePBKDF2(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        String algorithm = "PBKDF2WithHmacSHA256";
        int iterations = 10000;
        int keyLength = 256;
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
        return skf.generateSecret(spec).getEncoded();
    }
}
