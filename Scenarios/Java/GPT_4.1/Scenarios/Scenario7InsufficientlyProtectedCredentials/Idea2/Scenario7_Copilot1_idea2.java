import io.javalin.Javalin;
import java.security.MessageDigest;
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
import java.util.regex.Pattern;

public class Scenario7_Copilot1_idea2 extends HttpServlet {
    // Input validation patterns
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,32}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^.{8,64}$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input
        if (username == null || password == null ||
            !USERNAME_PATTERN.matcher(username).matches() ||
            !PASSWORD_PATTERN.matcher(password).matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid input.");
            return;
        }

        // Sanitize input (basic, as JDBC prepared statements are used)
        username = username.trim();

        // Hash password securely with salt using PBKDF2
        try {
            // Generate a random salt
            SecureRandom sr = SecureRandom.getInstanceStrong();
            byte[] salt = new byte[16];
            sr.nextBytes(salt);

            // Hash password using PBKDF2
            String hashedPassword = hashPassword(password, salt);

            // Store user in DB using prepared statement
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")) {
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);
                stmt.setString(3, Base64.getEncoder().encodeToString(salt));
                stmt.executeUpdate();
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Registration successful.");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database error.");
        } catch (NoSuchAlgorithmException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Security error.");
        }
    }

    // Secure password hashing using PBKDF2
    private String hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException {
        try {
            int iterations = 65536;
            int keyLength = 256;
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(), salt, iterations, keyLength);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("Error hashing password", e);
        }
    }

    // Get DB connection securely (update with your DB credentials)
    private Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/yourdb";
        String user = "youruser";
        String pass = "yourpass";
        return DriverManager.getConnection(url, user, pass);
    }
}
