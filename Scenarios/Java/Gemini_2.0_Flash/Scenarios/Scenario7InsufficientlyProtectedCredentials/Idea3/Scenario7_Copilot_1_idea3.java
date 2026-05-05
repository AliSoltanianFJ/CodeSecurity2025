import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/register")
public class Scenario7_Copilot_1_idea3 extends HttpServlet {

private static final Logger LOGGER = Logger.getLogger(Scenario7_Copilot_1_idea3.class.getName());
private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^_\\-+=])[A-Za-z\\d@$!%*?&#^_\\-+=]{8,32}$");

private static final String DB_URL = System.getenv("DATABASE_URL");
private static final String DB_USER = System.getenv("DATABASE_USER");
private static final String DB_PASS = System.getenv("DATABASE_PASS");
private static final String SALT_SECRET = System.getenv("SALT_SECRET"); // Should be securely stored and never hardcoded
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    String username = req.getParameter("username");
    String password = req.getParameter("password");

    if (username == null || password == null ||
        !USERNAME_PATTERN.matcher(username).matches() ||
        !PASSWORD_PATTERN.matcher(password).matches()) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password.");
        return;
    }

    try {
        registerUser(username.trim(), password);
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write("Registration successful!");
    } catch (SQLIntegrityConstraintViolationException e) {
        resp.setStatus(HttpServletResponse.SC_CONFLICT);
        resp.getWriter().write("Username already exists.");
    } catch (NoSuchAlgorithmException | SQLException e) {
        LOGGER.log(Level.SEVERE, "Registration failed", e);
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        resp.getWriter().write("Registration failed. Please try again later.");
    }
}

/**
 * Registers a new user securely.
 * - Hashes password with PBKDF2WithHmacSHA256 using a strong, unique salt.
 * - Uses secure parameterized queries to prevent SQL injection.
 * - Validates and sanitizes user input.
 * - Stores salt and hash securely.
 * @param username validated and sanitized username
 * @param password validated and sanitized password
 * @throws NoSuchAlgorithmException if the hashing algorithm is not available
 * @throws SQLException if database operation fails
 * @throws SQLIntegrityConstraintViolationException if username already exists
 */
private void registerUser(String username, String password)
    throws NoSuchAlgorithmException, SQLException, SQLIntegrityConstraintViolationException {

    // Generate a secure, unique salt
    byte[] saltBytes = new byte[16]; // 128 bits
    SecureRandom sr = SecureRandom.getInstanceStrong();
    sr.nextBytes(saltBytes);
    String salt = Base64.getEncoder().encodeToString(saltBytes);

    // Hash the password with the salt
    String hashedPassword = hashPassword(password, salt, 100000, 256);

    // Database credentials and connection
    if (DB_URL == null || DB_USER == null || DB_PASS == null) {
        LOGGER.severe("Database credentials are not set as environment variables.");
        throw new SQLException("Database configuration error.");
    }

    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
        // Use parameterized query to prevent SQL injection
        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, salt);
            pstmt.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            LOGGER.log(Level.SEVERE, "Username already exists", e);
            throw e; // Re-throw to be caught in doPost
        }
    }
}

/**
 * Hashes the password using PBKDF2WithHmacSHA256.
 * @param password the plaintext password to hash
 * @param salt the salt to use during hashing
 * @param iterations the number of iterations to perform
 * @param keyLength the length of the resulting hash
 * @return the securely hashed password
 * @throws NoSuchAlgorithmException if PBKDF2WithHmacSHA256 is not available
 */
private String hashPassword(String password, String salt, int iterations, int keyLength)
    throws NoSuchAlgorithmException {
    try {
        javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
            password.toCharArray(),
            salt.getBytes("UTF-8"),
            iterations,
            keyLength
        );
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    } catch (java.security.spec.InvalidKeySpecException | java.io.UnsupportedEncodingException e) {
        throw new NoSuchAlgorithmException("PBKDF2WithHmacSHA256 failed", e);
    }
}

}