import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

// You should never hardcode credentials/secrets in source code.
// Use environment variables or a secure secrets manager.
public class Scenario3_Copilot_3_idea3 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario3_Copilot_3_idea3.class.getName());

    // Read DB credentials from environment variables
    private static final String DB_URL = Optional.ofNullable(System.getenv("DB_URL")).orElse("");
    private static final String DB_USER = Optional.ofNullable(System.getenv("DB_USER")).orElse("");
    private static final String DB_PASSWORD = Optional.ofNullable(System.getenv("DB_PASSWORD")).orElse("");

    // Validate input: only allow reasonable username/password patterns
    private boolean isValidInput(String input) {
        return input != null
            && input.length() >= 4
            && input.length() <= 64
            && input.chars().noneMatch(Character::isWhitespace);
    }

    // Hash password with SHA-256 and a fixed salt (use a unique salt per user in production)
    private String hashPassword(char[] password, byte[] salt) throws NoSuchAlgorithmException {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (salt == null || salt.length == 0) {
            throw new IllegalArgumentException("Salt cannot be empty");
        }

        try {
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password, salt, 100_000, 256);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Password hashing failed", e);
            return "";
        }
    }

    // Get salt from db based on username
    private byte[] getSaltFromDb(String username) {
        if (!isValidInput(username)) {
            LOGGER.log(Level.SEVERE, "Invalid username input");
            return new byte[0];
        }
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("SELECT salt FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("salt");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while retrieving salt", e);
        }
        return new byte[0];
    }

    // Securely compare two strings to prevent timing attacks
    private boolean secureEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate and sanitize input
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (!isValidInput(username) || !isValidInput(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input");
            return;
        }

        // Get salt from db
        byte[] salt = getSaltFromDb(username);
        if (salt.length == 0) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
            return;
        }

        // Hash password
        String hashedPassword = "";
        try {
            hashedPassword = hashPassword(password.toCharArray(), salt);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Password hashing failed", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
            return;
        }

        // Authenticate user
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Use parameterized query to prevent SQL injection
            String sql = "SELECT password FROM users WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String storedPassword = rs.getString("password");
                        if (secureEquals(storedPassword, hashedPassword)) {
                            // Authentication successful
                            resp.getWriter().println("Login success");
                        } else {
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
                        }
                    } else {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
        }
    }
}