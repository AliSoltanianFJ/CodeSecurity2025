import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

public class GPT_4_1_Scenario7_Copilot1_FT_TOSP extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{5,30}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$");

    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null ||
            !USERNAME_PATTERN.matcher(username).matches() ||
            !PASSWORD_PATTERN.matcher(password).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }

        String sanitizedUsername = sanitize(username);

        String salt = generateSalt();
        String hashedPassword;
        try {
            hashedPassword = hashPassword(password, salt);
        } catch (NoSuchAlgorithmException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        }

        try {
            registerUser(sanitizedUsername, hashedPassword, salt);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Registration successful.");
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
        }
    }

    private static String sanitize(String input) {
        if (input == null) return "";
        return input.replaceAll("[<>'\"/;\\\\]", "");
    }

    private static String generateSalt() {
        byte[] salt = new byte[32];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private static String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt must not be null");
        }
        java.security.spec.KeySpec spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), 200000, 256);
        try {
            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (java.security.spec.InvalidKeySpecException e) {
            throw new NoSuchAlgorithmException("Error hashing password", e);
        }
    }

    private static void registerUser(String username, String passwordHash, String salt) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            String sql = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username.trim());
            pstmt.setString(2, passwordHash.trim());
            pstmt.setString(3, salt.trim());
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) {
                try { pstmt.close(); } catch (SQLException e) { /* log */ }
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { /* log */ }
            }
        }
    }
}
