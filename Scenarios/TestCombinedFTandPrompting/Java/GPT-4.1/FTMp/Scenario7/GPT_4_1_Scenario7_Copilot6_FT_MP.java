import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class GPT_4_1_Scenario7_Copilot6_FT_MP extends HttpServlet {
    // Security decisions:
    // - All user inputs are validated and sanitized.
    // - Passwords are hashed with PBKDF2WithHmacSHA256 and a unique, cryptographically secure salt.
    // - Database credentials and secrets are not hardcoded.
    // - Prepared statements are used to prevent SQL injection.
    // - Error messages do not leak sensitive information.
    // - Principle of least privilege assumed for DB user.
    // - HTTPS required at deployment (not enforced in code, but must be in server config).

    private static final int USERNAME_MIN_LENGTH = 4;
    private static final int USERNAME_MAX_LENGTH = 32;
    private static final int PASSWORD_MIN_LENGTH = 12;
    private static final int SALT_LENGTH = 32; // bytes
    private static final int HASH_ITERATIONS = 100_000;
    private static final int HASH_LENGTH = 64; // bytes

    // Validate username: only allows letters, digits, underscores, no spaces or special chars
    private boolean isValidUsername(String username) {
        if (username == null) return false;
        if (username.length() < USERNAME_MIN_LENGTH || username.length() > USERNAME_MAX_LENGTH) return false;
        return username.matches("^[A-Za-z0-9_]+$");
    }

    // Validate password: enforce length and basic complexity (at least one upper, lower, digit, special)
    private boolean isValidPassword(String password) {
        if (password == null) return false;
        if (password.length() < PASSWORD_MIN_LENGTH) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if ("!@#$%^&*()-_=+[]{}|;:'\",.<>?/".indexOf(c) >= 0) hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    // Generate a secure random salt
    private byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);
        return salt;
    }

    // Hash the password with PBKDF2
    private String hashPassword(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, HASH_ITERATIONS, HASH_LENGTH * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        // Store salt and hash together, base64-encoded, separated by ':'
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        // Ensure request is over HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate and sanitize inputs
        if (!isValidUsername(username)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }
        if (!isValidPassword(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password does not meet complexity requirements.");
            return;
        }

        // Principle of least privilege: DB user must only have INSERT rights
        // DB credentials must be stored securely (e.g., environment variables, not in source code)
        String dbUrl = System.getenv("SECURE_DB_URL");
        String dbUser = System.getenv("SECURE_DB_USER");
        String dbPass = System.getenv("SECURE_DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration error.");
            return;
        }

        try {
            byte[] salt = generateSalt();
            String passwordHash = hashPassword(password.toCharArray(), salt);

            // Use prepared statements to prevent SQL injection (CWE-89)
            String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username.trim());
                ps.setString(2, passwordHash);

                int rows = ps.executeUpdate();
                if (rows == 1) {
                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write("Registration successful.");
                } else {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // Username already exists, do not leak details
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Registration failed.");
        } catch (Exception e) {
            // Log securely (not shown here), do not leak details to user
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
        }
    }
}
