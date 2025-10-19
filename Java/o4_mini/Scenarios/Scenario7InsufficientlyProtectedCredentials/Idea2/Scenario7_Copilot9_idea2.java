import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

public class Scenario7_Copilot9_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("text/plain;charset=UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (!isValidUsername(username) || !isValidPassword(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password.");
            return;
        }

        try {
            register(username, password);
            resp.getWriter().println("Registration successful.");
        } catch (SQLException e) {
            log("Database error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
        }
    }

    private boolean isValidUsername(String username) {
        if (username == null) return false;
        // allow 3–20 alphanumerics or underscores
        return username.matches("^[A-Za-z0-9_]{3,20}$");
    }

    private boolean isValidPassword(String password) {
        if (password == null) return false;
        // require length between 8 and 64
        return password.length() >= 8 && password.length() <= 64;
    }

    private void register(String username, String password) throws SQLException {
        byte[] salt = generateSalt();
        byte[] hash = hashPassword(password.toCharArray(), salt);
        Arrays.fill(password.toCharArray(), '\0');

        String insert = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, username);
            ps.setString(2, toHex(hash));
            ps.setString(3, toHex(salt));
            ps.executeUpdate();
        }
    }

    private Connection getConnection() throws SQLException {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String pwd = System.getenv("DB_PASSWORD");
        return DriverManager.getConnection(url, user, pwd);
    }

    private byte[] generateSalt() {
        SecureRandom rnd = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        rnd.nextBytes(salt);
        return salt;
    }

    private byte[] hashPassword(char[] pwd, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(pwd, salt, ITERATIONS, KEY_LENGTH);
        Arrays.fill(pwd, '\0');
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing failed", e);
        } finally {
            spec.clearPassword();
        }
    }

    private String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
