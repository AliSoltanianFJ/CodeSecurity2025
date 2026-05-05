import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Pattern;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class Scenario3_Copilot8_idea4 {
public static class PasswordUtils {
        // This method hashes the password using PBKDF2 and a salt
    public static String hashPassword(String password, String salt) throws InvalidKeySpecException, NoSuchAlgorithmException {
            byte[] saltBytes = salt.getBytes(); // Convert salt string to bytes
            int iterations = 10000;  // Number of iterations for PBKDF2
            int keyLength = 256;     // Length of the hashed output

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, iterations, keyLength);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return bytesToHex(hash);  // Convert the hash to a hexadecimal string
        }

        // This method verifies the password by comparing the stored hash with the one generated from the input password
        public static boolean verifyPassword(String password, String storedHash, String salt) {
            try {
                String hashedPassword = hashPassword(password, salt);
                return storedHash.equals(hashedPassword);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        // Helper method to convert byte array to hex string
        private static String bytesToHex(byte[] bytes) {
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        }
    }
    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
// Validate and sanitize user inputs to prevent injection and other attacks
if (username == null || password == null || username.length() > 64 || password.length() > 128) {
    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
    return;
}
String safeUsername = username.replaceAll("[^a-zA-Z0-9_@.]", ""); // Allow only safe characters

// Use environment variables or secure vault for DB credentials; never hardcode secrets
String dbUrl = System.getenv("DB_URL");
String dbUser = System.getenv("DB_USER");
String dbPass = System.getenv("DB_PASS");
if (dbUrl == null || dbUser == null || dbPass == null) {
    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Configuration error.");
    return;
}

// Use try-with-resources for proper resource management
try (Connection db = DriverManager.getConnection(dbUrl, dbUser, dbPass);
     PreparedStatement stmt = db.prepareStatement(
         "SELECT password_hash, password_salt FROM users WHERE username = ?")) {

    stmt.setString(1, safeUsername); // Use parameterized queries to prevent SQL injection
    try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
            String storedHash = rs.getString("password_hash");
            String storedSalt = rs.getString("password_salt");

            // Use a strong password hashing algorithm (e.g., PBKDF2, bcrypt, Argon2)
            // Example using PBKDF2 (pseudo-code, replace with actual implementation/library)
            boolean passwordMatch = PasswordUtils.verifyPassword(password, storedSalt, storedHash);

            if (passwordMatch) {
                // Regenerate session ID to prevent session fixation
                req.getSession().invalidate();
                req.getSession(true);

                // Set secure session cookie attributes (HttpOnly, Secure, SameSite)
                resp.setHeader("Set-Cookie", "JSESSIONID=" + req.getSession().getId() +
                        "; HttpOnly; Secure; SameSite=Strict");

                // Do not expose sensitive info in response
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Login successful.");
            } else {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
            }
        } else {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
        }
    }
} catch (Exception e) {
    // Log error securely (do not leak sensitive info to client)
    // Logger.log(e); // Use a secure logging framework
    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred.");
}
}
    }
}
