// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario7_Copilot10_idea2 extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(Scenario7.class.getName());

    // PBKDF2 parameters
    private static final int SALT_LENGTH = 16; // bytes
    private static final int ITERATIONS = 200_000;
    private static final int KEY_LENGTH = 256; // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Ensure request encoding and response charset
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("text/plain; charset=UTF-8");

        String usernameRaw = req.getParameter("username");
        String passwordRaw = req.getParameter("password");

        // Basic null checks
        if (usernameRaw == null || passwordRaw == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Missing username or password.");
            return;
        }

        // Sanitize and validate inputs
        String username = sanitizeUsername(usernameRaw);
        String password = sanitizePassword(passwordRaw);

        if (!validateUsername(username) || !validatePassword(password)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Invalid username or password.");
            return;
        }

        // Avoid exposing DB credentials in code; read from environment
        String dbUrl = System.getenv("APP_DB_URL");
        String dbUser = System.getenv("APP_DB_USER");
        String dbPassword = System.getenv("APP_DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            LOG.log(Level.SEVERE, "Database configuration not provided in environment variables.");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Server configuration error.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            conn.setAutoCommit(false);
            // Check if user already exists
            if (userExists(conn, username)) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().println("User already exists.");
                return;
            }

            // Generate salt and hash the password using PBKDF2
            byte[] salt = generateSalt();
            byte[] hash = hashPassword(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);

            // Store user securely using prepared statement
            if (storeUser(conn, username, hashB64, saltB64, ITERATIONS)) {
                conn.commit();
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().println("User created.");
            } else {
                conn.rollback();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Failed to create user.");
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Database error during registration.", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Server error.");
        } catch (NoSuchAlgorithmException e) {
            LOG.log(Level.SEVERE, "Cryptography algorithm unavailable.", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Server error.");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unexpected error.", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Server error.");
        }
    }

    // Validate username: allow letters, digits, underscore, hyphen; length 3..30
    private boolean validateUsername(String username) {
        if (username == null) return false;
        if (username.length() < 3 || username.length() > 30) return false;
        return username.matches("^[A-Za-z0-9_-]+$");
    }

    // Validate password: length 8..128, require at least one digit and one letter
    private boolean validatePassword(String password) {
        if (password == null) return false;
        if (password.length() < 8 || password.length() > 128) return false;
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        return hasLetter && hasDigit;
    }

    // Sanitize username: trim and remove control characters and angle brackets to prevent XSS
    private String sanitizeUsername(String input) {
        if (input == null) return null;
        String s = input.trim();
        s = s.replaceAll("[\\p{Cntrl}<>\"'`\\\\]", "");
        // Enforce hard length limit to prevent very large input (mitigates buffer-like issues)
        if (s.length() > 256) s = s.substring(0, 256);
        return s;
    }

    // Sanitize password: trim control chars; do NOT log or echo password
    private String sanitizePassword(String input) {
        if (input == null) return null;
        String s = input.trim();
        s = s.replaceAll("[\\p{Cntrl}<>\"'`\\\\]", "");
        if (s.length() > 512) s = s.substring(0, 512);
        return s;
    }

    private byte[] generateSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);
        return salt;
    }

    private byte[] hashPassword(final char[] password, final byte[] salt, final int iterations, final int keyLength)
            throws NoSuchAlgorithmException {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            // Wrap exceptions as NoSuchAlgorithmException for callers
            throw new NoSuchAlgorithmException("Failed to hash password.", e);
        }
    }

    private boolean userExists(Connection conn, String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean storeUser(Connection conn, String username, String hashB64, String saltB64, int iterations) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, salt, iterations, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.setInt(4, iterations);
            int rows = ps.executeUpdate();
            return rows == 1;
        }
    }
}
// ...existing code...
