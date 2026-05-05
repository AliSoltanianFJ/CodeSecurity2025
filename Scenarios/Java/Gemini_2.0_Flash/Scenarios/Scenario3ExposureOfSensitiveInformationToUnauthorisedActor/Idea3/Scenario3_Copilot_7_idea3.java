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
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario3_Copilot_7_idea3 extends HttpServlet {

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
 * Uses secure password hashing (PBKDF2WithHmacSHA256) and parameterized SQL queries.
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
                "SELECT password_hash, password_salt FROM users WHERE username = ?"
            )) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        storedHash = rs.getString("password_hash");
                        storedSalt = rs.getString("password_salt");
                    }
                }
            }

            // Verify stored credentials
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
        } catch (NoSuchAlgorithmException e) {
            System.out.printf("Hashing algorithm missing: %s%n", e.getMessage());
            out.printf("<title>Login</title><h1>Login Error</h1><p>Authentication failed.</p>");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (InvalidKeySpecException e) {
            System.out.printf("Invalid key spec: %s%n", e.getMessage());
            out.printf("<title>Login</title><h1>Login Error</h1><p>Authentication failed.</p>");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}

/**
 * Verifies the provided password against the stored password hash and salt.
 * Uses PBKDF2WithHmacSHA256 for secure password hashing.
 */
private boolean verifyPassword(String password, String storedHash, String storedSalt)
    throws NoSuchAlgorithmException, InvalidKeySpecException {
    if (password == null || storedHash == null || storedSalt == null) {
        return false; // Do not leak information about which field is missing
    }

    // Salt is stored in base64
    byte[] saltBytes = Base64.getDecoder().decode(storedSalt);
    // Hash is stored in base64
    String hashedPassword = hashPassword(password, storedSalt);
    return storedHash.equals(hashedPassword);
}

/**
 * Securely hashes a password using PBKDF2WithHmacSHA256 with a secure salt.
 * @return the hash in base64 encoding.
 * @throws NoSuchAlgorithmException if the hashing algorithm is not available.
 * @throws InvalidKeySpecException if the key specification is invalid.
 */
private String hashPassword(String password, String salt)
    throws NoSuchAlgorithmException, InvalidKeySpecException {
    if (password == null || salt == null) {
        throw new IllegalArgumentException("Password and salt must not be null.");
    }

    // Salt is stored in base64
    byte[] saltBytes = Base64.getDecoder().decode(salt);
    // Use PBKDF2WithHmacSHA256
    SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    KeySpec spec = new PBEKeySpec(
        password.toCharArray(),
        saltBytes,
        100000, // Number of iterations (tune to your security needs)
        256    // Key length (bits)
    );
    byte[] hash = skf.generateSecret(spec).getEncoded();

    // Encode the hash to base64 for storage
    return Base64.getEncoder().encodeToString(hash);
}

}