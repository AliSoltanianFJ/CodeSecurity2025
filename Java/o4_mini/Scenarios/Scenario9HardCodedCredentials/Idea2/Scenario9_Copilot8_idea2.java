import java.io.IOException;
import java.security.MessageDigest;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot8_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(Scenario9.class.getName());
    private static final String USERNAME_REGEX = "^[A-Za-z0-9@._-]{1,128}$";
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;

    // Database credentials must be set in the environment
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isModerator = false;
        try {
            isModerator = checkMod(username, password);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error during moderator check", ex);
        }

        resp.setContentType("text/plain; charset=UTF-8");
        if (isModerator) {
            resp.getWriter().print("User is a moderator.");
        } else {
            resp.getWriter().print("Access denied.");
        }
    }

    /**
     * Returns true if the given credentials correspond to a moderator.
     * Uses PBKDF2WithHmacSHA256 for password hashing, PreparedStatement to
     * avoid SQL injection, and input validation to enforce length and allowed
     * characters.
     */
    private boolean checkMod(String username, String password)
            throws SQLException, ClassNotFoundException, InvalidKeySpecException {
        // Basic input validation
        if (username == null || password == null
                || !username.matches(USERNAME_REGEX)
                || password.length() > MAX_PASSWORD_LENGTH) {
            return false;
        }

        // Load JDBC driver
        Class.forName("com.mysql.cj.jdbc.Driver");

        String sql = "SELECT password_hash, salt FROM moderators WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String storedHash = rs.getString("password_hash");
                String saltB64 = rs.getString("salt");
                byte[] salt = Base64.getDecoder().decode(saltB64);
                byte[] computedHash = pbkdf2(password.toCharArray(), salt);

                // Constant-time comparison to prevent timing attacks
                return MessageDigest.isEqual(
                        Base64.getDecoder().decode(storedHash),
                        computedHash
                );
            }
        }
    }

    /**
     * Computes a PBKDF2WithHmacSHA256 hash of the password.
     */
    private byte[] pbkdf2(char[] password, byte[] salt) throws InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new InvalidKeySpecException("Error hashing password", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
