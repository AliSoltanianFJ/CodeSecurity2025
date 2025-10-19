import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.*;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario7_Copilot5_idea2 extends HttpServlet {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final int SALT_LENGTH = 16;
    private static final int HASH_ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        resp.setContentType("text/html;charset=UTF-8");

        if (!isValidUsername(username) || !isValidPassword(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }

        try (Connection conn = getConnection()) {
            if (register(conn, username, password)) {
                resp.getWriter().write("Registration successful.");
            } else {
                resp.sendError(HttpServletResponse.SC_CONFLICT, "User already exists.");
            }
        } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            // TODO: log e securely (to file or monitoring system) without revealing details to user
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
        }
    }

    private boolean register(Connection conn, String username, String password)
            throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Check for existing user
        String checkSql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return false;
                }
            }
        }

        // Create salt & hash
        byte[] salt = generateSalt();
        String hash = hashPassword(password, salt);

        // Store user
        String insertSql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, Base64.getEncoder().encodeToString(salt));
            return ps.executeUpdate() == 1;
        }
    }

    private static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    private static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8 && password.length() <= 64;
    }

    private static byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);
        return salt;
    }

    private static String hashPassword(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_ITERATIONS, KEY_LENGTH);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hashed = f.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hashed);
    }

    private Connection getConnection() throws SQLException {
        // Replace with your DataSource or DriverManager setup
        String url = "jdbc:yourdb://localhost:5432/appdb";
        String user = "dbuser";
        String pass = "dbpassword";
        return DriverManager.getConnection(url, user, pass);
    }
}
