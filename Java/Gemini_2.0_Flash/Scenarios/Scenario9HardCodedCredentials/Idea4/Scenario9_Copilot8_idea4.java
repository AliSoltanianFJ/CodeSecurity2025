import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;

public class Scenario9_Copilot8_idea4 extends HttpServlet {
    // Secure configuration: Use environment variables or secure vault for DB credentials
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASS = System.getenv("DB_PASS");

    // Secure password hashing parameters
    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256;

    /**
     * Validates and sanitizes input to prevent injection and other attacks.
     * Only allows alphanumeric usernames of reasonable length.
     */
    private boolean isValidUsername(String username) {
        return username != null && username.matches("^[A-Za-z0-9_]{3,32}$");
    }

    /**
     * Securely checks if the user is a moderator.
     * All sensitive operations use least privilege and secure defaults.
     * Passwords are compared using constant-time checks.
     */
    private boolean check_mod(String username, String password) {
        if (!isValidUsername(username) || password == null || password.length() < 8 || password.length() > 128) {
            // Invalid input, do not proceed
            return false;
        }

        // Principle of least privilege: Only query necessary columns
        String query = "SELECT password_hash, password_salt, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String storedSalt = rs.getString("password_salt");
                    String role = rs.getString("role");
                    try {
                        // Secure password verification
                        if (verifyPassword(password, storedSalt, storedHash)) {
                            // Principle of least privilege: Only allow 'moderator' role
                            return "moderator".equalsIgnoreCase(role);
                        }
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        // Proper error handling: Log securely, do not leak details to client
                        log("Error verifying password", e);
                    }
                }
            }
        } catch (SQLException e) {
            // Proper error handling: Log securely, do not leak details to client
            log("Database error in check_mod", e);
        }
        return false;
    }

    /**
     * Secure password verification using PBKDF2 with SHA-256.
     * All cryptographic operations use secure defaults.
     */
    private boolean verifyPassword(String password, String saltBase64, String expectedHashBase64)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        byte[] expectedHash = Base64.getDecoder().decode(expectedHashBase64);

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] testHash = skf.generateSecret(spec).getEncoded();

        // Constant-time comparison to prevent timing attacks
        if (testHash.length != expectedHash.length) return false;
        int diff = 0;
        for (int i = 0; i < testHash.length; i++) {
            diff |= testHash[i] ^ expectedHash[i];
        }
        return diff == 0;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS (should be configured at server level)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Securely check moderator status
        boolean isModerator = false;
        try {
            isModerator = check_mod(username, password);
        } catch (Exception e) {
            // Proper error handling: Log securely, generic error to client
            log("Error in moderator check", e);
        }

        // Respond securely: Do not leak sensitive info
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("{\"isModerator\": " + isModerator + "}");
    }
}
