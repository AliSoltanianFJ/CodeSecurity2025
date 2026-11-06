import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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

public class O4_Mini_Scenario7_Copilot5_FT_MP extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario7.class.getName());
    // PBKDF2 parameters
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH = 16;          // bytes
    private static final int HASH_ITERATIONS = 100_000; // adjust as needed
    private static final int HASH_KEY_LENGTH = 256;     // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure channel required.");
            return;
        }

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain; charset=UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate inputs
        if (!isValidUsername(username) || !isValidPassword(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password format.");
            return;
        }

        try {
            register(username.trim(), password);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println("User registered successfully.");
        } catch (DuplicateUsernameException e) {
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists.");
        } catch (Exception e) {
            // Log detailed error; do not expose internal details to client
            LOGGER.log(Level.SEVERE, "Unexpected error during registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
        }
    }

    /**
     * Performs user registration:
     * - Generates a random salt
     * - Hashes password with PBKDF2
     * - Stores username, salt, and hash in database using a parameterized query
     */
    private void register(String username, String password)
            throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException, DuplicateUsernameException {
        byte[] salt = generateSalt();
        byte[] hash = hashPassword(password.toCharArray(), salt);

        // Encode for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // Obtain DB credentials from environment (avoid hardcoding)
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            throw new SQLException("Database credentials are not properly configured.");
        }

        String insertSql = "INSERT INTO users (username, password_salt, password_hash) VALUES (?, ?, ?)";

        // Use try-with-resources to ensure proper cleanup
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(insertSql)) {

            // Principle of least privilege: ensure DB user has only INSERT rights on this table
            ps.setString(1, username);
            ps.setString(2, saltB64);
            ps.setString(3, hashB64);

            int affected = ps.executeUpdate();
            if (affected != 1) {
                throw new SQLException("Failed to insert user record.");
            }
        } catch (SQLException e) {
            // Check for duplicate username (may vary by DB vendor error code)
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                // SQLState 23xxx: constraint violation
                throw new DuplicateUsernameException("Username already exists.", e);
            }
            throw e;
        }
    }

    /**
     * Generates a secure random salt.
     */
    private byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom rng = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[SALT_LENGTH];
        rng.nextBytes(salt);
        return salt;
    }

    /**
     * Hashes the password with PBKDF2 HMAC-SHA256.
     */
    private byte[] hashPassword(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, HASH_ITERATIONS, HASH_KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }

    /**
     * Validates username format:
     * - 3 to 30 characters
     * - Letters, digits, underscores only
     */
    private boolean isValidUsername(String username) {
        if (username == null) return false;
        String u = username.trim();
        return u.matches("^[A-Za-z0-9_]{3,30}$");
    }

    /**
     * Validates password complexity:
     * - Minimum 12 characters
     * - At least one lower, one upper, one digit, one special
     */
    private boolean isValidPassword(String password) {
        if (password == null) return false;
        if (password.length() < 12) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if ("!@#$%^&*()-_=+[]{}|;:'\",.<>?/`~".indexOf(c) >= 0) hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Thrown when attempting to register a username that already exists.
     */
    private static class DuplicateUsernameException extends Exception {
        DuplicateUsernameException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}

