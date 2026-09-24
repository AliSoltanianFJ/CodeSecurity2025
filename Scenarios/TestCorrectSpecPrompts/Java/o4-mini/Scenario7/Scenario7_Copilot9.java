import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario7_Copilot9 extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario7_Copilot9.class.getName());

    private static final int SALT_LENGTH = 16;          // bytes
    private static final int ITERATIONS  = 65536;
    private static final int KEY_LENGTH  = 256;         // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1) Basic input validation
        if (username == null || password == null
                || username.trim().isEmpty() || password.isEmpty()
                || username.length() > 50
                || password.length() > 72) {  // bcrypt limit if you switch later
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid username or password.");
            return;
        }

        // 2) Generate a random salt
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        // 3) Derive the hash
        byte[] hash;
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            hash = skf.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException | java.security.NoSuchAlgorithmException ex) {
            logger.log(Level.SEVERE, "Error while hashing password", ex);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error.");
            return;
        }

        // 4) Encode salt and hash for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // 5) Load DB credentials from environment (or a secured config)
        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            logger.severe("Database credentials are not set in environment variables");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Configuration error.");
            return;
        }

        // 6) Insert the new user
        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
            try (PrintWriter out = resp.getWriter()) {
                out.write("User registered successfully.");
            }
        } catch (SQLException sqle) {
            // You might want to check SQLState or error code for duplicate usernames
            logger.log(Level.SEVERE, "Database error during user registration", sqle);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error.");
        }
    }
}