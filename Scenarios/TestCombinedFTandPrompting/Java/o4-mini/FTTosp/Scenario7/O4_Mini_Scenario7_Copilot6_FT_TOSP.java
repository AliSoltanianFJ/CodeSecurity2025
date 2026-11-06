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
import java.sql.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class O4_Mini_Scenario7_Copilot6_FT_TOSP extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario7.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. Read and trim parameters
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username != null) username = username.trim();
        if (password != null) password = password.trim();

        // 2. Validate input
        if (username == null || password == null
                || username.isEmpty() || password.isEmpty()
                || username.length() < 3 || username.length() > 30
                || password.length() < 8 || password.length() > 64
                || !username.matches("^[A-Za-z0-9_]+$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password");
            return;
        }

        // 3. Load DB credentials from environment (never hard-code)
        String dbUrl      = System.getenv("DB_URL");
        String dbUser     = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            logger.severe("Database credentials are not configured");
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Configuration error");
            return;
        }

        // 4. Prepare hashing parameters
        final int    ITERATIONS = 65536;
        final int    KEY_LENGTH = 256; // bits
        final SecureRandom rng    = new SecureRandom();
        byte[]       salt       = new byte[16];
        rng.nextBytes(salt);

        char[]       pwdChars = password.toCharArray();
        byte[]       hashBytes;
        try {
            PBEKeySpec spec = new PBEKeySpec(pwdChars, salt, ITERATIONS, KEY_LENGTH);
            Arrays.fill(pwdChars, '\0');
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            hashBytes = skf.generateSecret(spec).getEncoded();
            spec.clearPassword();
        } catch (InvalidKeySpecException | java.security.NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Error while hashing password", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            return;
        }

        // 5. Encode salt and hash for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hashBytes);
        Arrays.fill(hashBytes, (byte) 0);

        // 6. Database operations
        String checkSql  = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        String insertSql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            // 6.a Check for existing user
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists");
                        return;
                    }
                }
            }

            // 6.b Insert new user
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, username);
                ps.setString(2, hashB64);
                ps.setString(3, saltB64);
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            return;
        }

        // 7. Return success (no echo of PII)
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.setContentType("application/json;charset=UTF-8");
        try (PrintWriter w = resp.getWriter()) {
            w.write("{\"message\":\"User registered successfully\"}");
        }
    }
}

