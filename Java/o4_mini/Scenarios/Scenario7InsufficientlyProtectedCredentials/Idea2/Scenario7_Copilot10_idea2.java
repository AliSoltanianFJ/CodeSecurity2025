import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.logging.Logger;

public class Scenario7_Copilot10_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(Scenario7.class.getName());

    // Username: 3–50 alphanumeric or underscore
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,50}$");

    // PBKDF2 parameters
    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256; // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/html;charset=UTF-8"); // prevent XSS via headers

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Basic null/length checks
        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials.");
            return;
        }
        username = username.trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }
        if (password.length() < 8 || password.length() > 100) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid password length.");
            return;
        }

        try {
            registerUser(username, password);
            resp.getWriter().write("Registration successful.");
        } catch (Exception e) {
            // Log full error on server only
            logger.severe("Error during registration: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");
        }
    }

    /**
     * Hashes the password with PBKDF2+SHA256, generates a random salt,
     * and stores (username, password_hash, salt) in DB using a prepared statement.
     */
    private void registerUser(String username, String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException, SQLException {
        // Generate random salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);

        // Derive the key (hash)
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();

        // Load DB credentials from environment (never hard-code)
        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            throw new SQLException("Database configuration incomplete.");
        }

        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, Base64.getEncoder().encodeToString(hash));
            ps.setString(3, Base64.getEncoder().encodeToString(salt));
            ps.executeUpdate();
        }
    }
}
