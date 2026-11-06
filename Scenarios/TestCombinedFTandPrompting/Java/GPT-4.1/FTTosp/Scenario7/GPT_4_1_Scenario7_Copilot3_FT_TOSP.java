import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;

public class GPT_4_1_Scenario7_Copilot3_FT_TOSP extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        // -copilot next line-
        try {
            register(username, password);
        } catch (NoSuchAlgorithmException | SQLException e) {
            throw new ServletException("Registration failed. Please try again later.");
        }
    }

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,32}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,64}$");

    private void register(String username, String password) throws NoSuchAlgorithmException, SQLException, ServletException {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new ServletException("Invalid username format.");
        }
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ServletException("Password does not meet complexity requirements.");
        }

        byte[] salt = new byte[16];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);

        char[] passwordChars = password.toCharArray();
        byte[] hashedPassword;
        try {
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(passwordChars, salt, 100_000, 256);
            hashedPassword = skf.generateSecret(spec).getEncoded();
            spec.clearPassword();
        } catch (Exception e) {
            throw new ServletException("Internal error during password hashing.");
        } finally {
            java.util.Arrays.fill(passwordChars, '\0');
        }

        String dbUrl = System.getenv("SECURE_DB_URL");
        String dbUser = System.getenv("SECURE_DB_USER");
        String dbPass = System.getenv("SECURE_DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            throw new ServletException("Database credentials not configured securely.");
        }

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            String query = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, username.trim());
                ps.setString(2, Base64.getEncoder().encodeToString(hashedPassword));
                ps.setString(3, Base64.getEncoder().encodeToString(salt));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                throw new ServletException("Username already exists.");
            } else {
                throw new ServletException("Registration failed due to a server error.");
            }
        } finally {
            java.util.Arrays.fill(hashedPassword, (byte) 0);
            java.util.Arrays.fill(salt, (byte) 0);
        }
    }
}
