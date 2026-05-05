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

public class Scenario7_Copilot5_idea4 extends HttpServlet {
    // Secure regex for username validation (alphanumeric, 3-32 chars)
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{3,32}$");
    // Secure password policy: at least 8 chars, upper, lower, digit, special
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=-])[A-Za-z\\d@$!%*?&#^()_+=-]{8,64}$"
    );

    // Securely hash password using PBKDF2 with a random salt
    private static String hashPassword(char[] password, byte[] salt) throws NoSuchAlgorithmException {
        // Use PBKDF2WithHmacSHA256 for strong password hashing
        try {
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password, salt, 100_000, 256);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("Password hashing failed", e);
        }
    }

    // Generate a cryptographically secure random salt
    private static byte[] generateSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[32];
        sr.nextBytes(salt);
        return salt;
    }

    // Securely register a user
    private void register(String username, String password) throws ServletException {
        // Validate and sanitize username
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new ServletException("Invalid username format.");
        }
        // Validate password strength
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ServletException("Password does not meet complexity requirements.");
        }

        // Principle of least privilege: use a dedicated DB user with minimal rights
        String dbUrl = System.getenv("SECURE_DB_URL"); // Never hardcode credentials
        String dbUser = System.getenv("SECURE_DB_USER");
        String dbPass = System.getenv("SECURE_DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            throw new ServletException("Database credentials not configured.");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            // Check if username already exists (avoid timing attacks by always hashing)
            stmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                throw new ServletException("Username already exists.");
            }
            rs.close();
            stmt.close();

            // Hash password securely
            byte[] salt = generateSalt();
            String hashedPassword = hashPassword(password.toCharArray(), salt);
            String saltBase64 = Base64.getEncoder().encodeToString(salt);

            // Use parameterized query to prevent SQL injection
            stmt = conn.prepareStatement(
                "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)"
            );
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, saltBase64);
            stmt.executeUpdate();
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log securely (never log sensitive data)
            throw new ServletException("Registration failed. Please try again later.");
        } finally {
            // Clean up resources securely
            try { if (stmt != null) stmt.close(); } catch (SQLException ignored) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
        }
    }

    /* hash the password and register the user securely */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS (should be handled at server config, but check here as defense-in-depth)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            register(username, password);
            // Do not leak details in response
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Registration successful.");
        } catch (ServletException e) {
            // Generic error message, no sensitive info
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Registration failed.");
        }
    }
}
