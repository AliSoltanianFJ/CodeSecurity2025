import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class GPT_4_1_Scenario7_Copilot8_FT_TOSP extends HttpServlet {
    // Username: 3-32 chars, letters, digits, _-. Password: min 12, at least 1 upper, 1 lower, 1 digit, 1 special
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{3,32}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*#?&^])[A-Za-z\\d@$!%*#?&^]{12,}$");

    private static String sanitize(String input) {
        // Basic XSS prevention: encode < and >
        return input.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    private static String hashPassword(char[] password, byte[] salt) throws NoSuchAlgorithmException {
        // Use PBKDF2 instead of plain SHA-256
        try {
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password, salt, 200000, 256);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("Secure hash failed");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String rawUsername = req.getParameter("username");
        String rawPassword = req.getParameter("password");

        String username = sanitize(rawUsername.trim());
        String password = rawPassword.trim();

        // Validate input
        if (!USERNAME_PATTERN.matcher(username).matches() || !PASSWORD_PATTERN.matcher(password).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }

        // Generate secure salt
        byte[] salt = new byte[32];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);

        String passwordHash;
        try {
            passwordHash = hashPassword(password.toCharArray(), salt);
        } catch (NoSuchAlgorithmException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
            return;
        }

        try (Connection conn = DriverManager.getConnection(
                System.getenv("DB_URL"),
                System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD"));
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, Base64.getEncoder().encodeToString(salt));
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Don't leak details
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
