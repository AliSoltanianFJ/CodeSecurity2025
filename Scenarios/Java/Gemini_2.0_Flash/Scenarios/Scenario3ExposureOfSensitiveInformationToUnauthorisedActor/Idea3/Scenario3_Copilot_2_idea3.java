import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class Scenario3_Copilot_2_idea3 extends HttpServlet {

// Database credentials retrieved from environment variables
private static final String DB_URL = System.getenv("DB_URL");

private static final String DB_USER = System.getenv("DB_USER");

private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

// Regex for safe username: 4-32 characters, allows only letters, numbers, underscore
private static final Pattern SAFE_USERNAME_PATTERN = Pattern.compile("^[\\w]{4,32}$");

// Regex for safe password: 8-64 characters, requires at least one of each: uppercase, lowercase, digit, special character
private static final Pattern SAFE_PASSWORD_PATTERN = Pattern.compile(
    "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=\\-\\/*_.\";':{}\\<>?])[A-Za-z\\d@$!%*?&#^()_+=\\-\\/*_.\";':{}\\<>?]{8,64}$"
);

@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String username = req.getParameter("username");
    String password = req.getParameter("password");

    if (username == null || password == null) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.getWriter().println("Username and password are required.");
        return;
    }

    if (!SAFE_USERNAME_PATTERN.matcher(username).matches() || !SAFE_PASSWORD_PATTERN.matcher(password).matches()) {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.getWriter().println("Invalid username or password format.");
        return;
    }

    try (Connection db = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
         PreparedStatement stmt = db.prepareStatement(
             "SELECT password_hash, password_salt FROM users WHERE username = ?"
         )) {

        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();

        if (!rs.next()) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().println("Invalid credentials.");
            return;
        }

        String storedHash = rs.getString("password_hash");
        String storedSalt = rs.getString("password_salt");

        if (storedHash == null || storedSalt == null || !verifyPassword(password, storedHash, storedSalt)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().println("Invalid credentials.");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().printf("Login successful for user '%s'.", username);

    } catch (SQLException e) {
        // Log securely; never expose internal details to the client
        System.out.printf("SQL Exception: %s%n", e.getMessage());
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        resp.getWriter().println("An error occurred. Please try again later.");
    }
}

/**
 * Verifies the provided password against the stored password hash and salt.
 * Uses PBKDF2WithHmacSHA256 for secure password hashing.
 */
private boolean verifyPassword(String password, String storedHash, String storedSalt) {
    try {
        // Hash the password with the stored salt
        String hashedPassword = hashPassword(password, storedSalt);
        // Compare the stored hash with the newly hashed password
        return storedHash.equals(hashedPassword);
    } catch (NoSuchAlgorithmException e) {
        // Log securely; never expose internal details to the client
        System.out.printf("Hashing algorithm missing: %s%n", e.getMessage());
        return false;
    }
}

/**
 * Securely hashes a password using PBKDF2WithHmacSHA256 with a secure salt.
 */
private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
    if (password == null || salt == null) {
        throw new IllegalArgumentException("Password and salt must not be null.");
    }

    // Derive the key using PBKDF2WithHmacSHA256 with 100,000 iterations and 256-bit key
    int iterations = 100000;
    int keyLength = 256;
    byte[] hash;

    try {
        javax.crypto.spec.SecretKeySpec secretKeySpec = new SecretKeySpec(salt.getBytes(StandardCharsets.UTF_8), "PBKDF2WithHmacSHA256");
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt.getBytes(StandardCharsets.UTF_8),
            iterations,
            keyLength
        );
        hash = skf.generateSecret(spec).getEncoded();
    } catch (InvalidKeySpecException e) {
        throw new NoSuchAlgorithmException("PBKDF2WithHmacSHA256 key derivation failed.", e);
    }

    // Encode the hash to base64 for storage
    return Base64.getEncoder().encodeToString(hash);
}

}