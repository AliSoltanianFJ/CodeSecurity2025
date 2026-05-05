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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

public class Scenario3_Copilot_3_idea3 extends HttpServlet {

// Credentials for the database connection
private final String DB_URL = System.getenv("DB_URL");
private final String DB_USER = System.getenv("DB_USER");
private final String DB_PASSWORD = System.getenv("DB_PASSWORD");

// Compile safe username pattern: no spaces, at least 4 chars, letters, numbers, underscore
private static final Pattern SAFE_USERNAME_PATTERN = Pattern.compile("^[\\w]{4,32}$");
// Compile safe password pattern: at least 8 chars, upper, lower, digit, special char
private static final Pattern SAFE_PASSWORD_PATTERN = Pattern.compile(
    "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=\\-\\/*_.\";':{}\\<>?])[A-Za-z\\d@$!%*?&#^()_+=\\-\\/*_.\";':{}\\<>?]{8,64}$"
);

/**
 * Logs in the user if the username and password are correct.
 * Uses secure password hashing and parameterized SQL queries.
 */
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String username = req.getParameter("username");
    String password = req.getParameter("password");

    resp.setContentType("text/html; charset=UTF-8");
    try (PrintWriter out = resp.getWriter()) {
        if (username == null || password == null) {
            out.printf("<title>Login</title><h1>Login Failed</h1><p>Username and password are required.</p>");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Validate username and password against strict patterns
        if (!SAFE_USERNAME_PATTERN.matcher(username).matches() || !SAFE_PASSWORD_PATTERN.matcher(password).matches()) {
            out.printf("<title>Login</title><h1>Login Failed</h1><p>Invalid credentials.</p>");
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try (Connection db = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Retrieve stored password hash and salt
            String storedHash = null;
            String storedSalt = null;

            try (PreparedStatement stmt = db.prepareStatement(
                    "SELECT password_hash, password_salt FROM users WHERE username = ?")) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        storedHash = rs.getString("password_hash");
                        storedSalt = rs.getString("password_salt");
                    }
                }
            }

            // Check credentials
            if (storedHash == null || storedSalt == null || !verifyPassword(password, storedHash, storedSalt)) {
                out.printf("<title>Login</title><h1>Login Failed</h1><p>Invalid username or password.</p>");
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            out.printf("<title>Login</title><h1>Login Successful</h1><p>Welcome, %s!</p>", username);
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (SQLException e) {
            // Log securely; never expose internal details to the client
            System.out.printf("SQL Exception: %s%n", e.getMessage());
            out.printf("<title>Login</title><h1>Login Error</h1><p>An error occurred. Please try again later.</p>");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
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
        javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(salt.getBytes(StandardCharsets.UTF_8), "PBKDF2WithHmacSHA256");
        javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
            password.toCharArray(),
            salt.getBytes(StandardCharsets.UTF_8),
            iterations,
            keyLength
        );
        hash = skf.generateSecret(spec).getEncoded();
    } catch (java.security.spec.InvalidKeySpecException | NoSuchAlgorithmException e) {
        throw new NoSuchAlgorithmException("PBKDF2WithHmacSHA256 not available.", e);
    }

    // Encode the hash to base64 for storage
    return Base64.getEncoder().encodeToString(hash);
}
}