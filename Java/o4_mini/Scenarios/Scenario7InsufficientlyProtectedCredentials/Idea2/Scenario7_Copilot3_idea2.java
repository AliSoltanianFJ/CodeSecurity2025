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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario7_Copilot3_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // PBKDF2 parameters
    private static final int ITERATIONS   = 65_536;
    private static final int KEY_LENGTH   = 256;
    private static final SecureRandom RNG = new SecureRandom();

    // Username must be 3–20 chars, alphanumeric + underscore
    private static final Pattern USERNAME_PATTERN =
        Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1) Validate and trim inputs
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username != null) username = username.trim();
        if (password != null) password = password.trim();

        if (username == null
            || password == null
            || !USERNAME_PATTERN.matcher(username).matches()
            || password.length() < 8
            || password.length() > 64) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password.");
            return;
        }

        try {
            register(username, password);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().println("Registration successful.");
        } catch (Exception e) {
            log("Registration error for user " + username, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "An internal error occurred.");
        }
    }

    /**
     * Registers a new user by hashing their password with PBKDF2,
     * then storing username, salt, and hash in the database.
     */
    private void register(String username, String password)
            throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        // 2) Generate per-user random salt
        byte[] salt = new byte[16];
        RNG.nextBytes(salt);

        // 3) Derive the hash
        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();

        // 4) Base64-encode both
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // 5) Load DB credentials from environment
        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        // 6) Store in DB with a prepared statement
        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.executeUpdate();
        }
    }
}
