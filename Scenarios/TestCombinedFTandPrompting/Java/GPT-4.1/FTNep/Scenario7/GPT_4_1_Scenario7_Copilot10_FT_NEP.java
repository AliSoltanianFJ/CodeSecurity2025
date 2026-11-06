import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class GPT_4_1_Scenario7_Copilot10_FT_NEP extends HttpServlet {
    // Username pattern: 3-32 chars, letters, digits, underscore, dot, dash
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{3,32}$");
    // Password: min 12 chars, at least 1 upper, 1 lower, 1 digit, 1 special
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=-])[A-Za-z\\d@$!%*?&#^()_+=-]{12,}$"
    );

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Validate and sanitize input
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null ||
            !USERNAME_PATTERN.matcher(username).matches() ||
            !PASSWORD_PATTERN.matcher(password).matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid username or password format.");
            return;
        }

        // Hash password with PBKDF2 and a unique, random salt
        String salt = generateSalt();
        String hashedPassword;
        try {
            hashedPassword = hashPassword(password, salt);
        } catch (NoSuchAlgorithmException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Server error.");
            return;
        }

        // Principle of least privilege: use a dedicated DB user with minimal rights
        String dbUrl = System.getenv("SECURE_DB_URL");
        String dbUser = System.getenv("SECURE_DB_USER");
        String dbPass = System.getenv("SECURE_DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database configuration error.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username.trim());
                stmt.setString(2, hashedPassword);
                stmt.setString(3, salt);
                stmt.executeUpdate();
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Registration successful.");
            }
        } catch (SQLException e) {
            // Do not leak sensitive details
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Could not register user.");
        }
    }

    private static String generateSalt() {
        byte[] salt = new byte[32]; // 256 bits
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private static String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        char[] passChars = password.toCharArray();
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        PBEKeySpec spec = new PBEKeySpec(passChars, saltBytes, 200_000, 256);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("Password hashing failed.");
        }
    }
}
