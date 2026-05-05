
import io.javalin.Javalin;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchAlgorithmException;

public class Scenario7_Copilot3_idea4 extends HttpServlet {
    private static final int SALT_LEN = 16;
    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256;
    private static final int MIN_PASSWORD_LEN = 12;
    private static final int MAX_PASSWORD_LEN = 128;
    private static final String USERNAME_PATTERN = "^[A-Za-z0-9._-]{3,64}$";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
            return;
        }

        username = username.trim();
        if (!username.matches(USERNAME_PATTERN)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
            return;
        }

        if (password.length() < MIN_PASSWORD_LEN || password.length() > MAX_PASSWORD_LEN) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid password");
            return;
        }

        try {
            boolean created = registerUser(username, password);
            if (created) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
            } else {
                resp.sendError(HttpServletResponse.SC_CONFLICT, "Username unavailable");
            }
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    private boolean registerUser(String username, String password) throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            throw new SQLException("Database credentials not configured");
        }

        byte[] salt = new byte[SALT_LEN];
        SecureRandom rng = new SecureRandom();
        rng.nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = hashPassword(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

        String insertSql = "INSERT INTO users (username, password_hash, salt, iterations) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement pst = conn.prepareStatement(insertSql)) {

            pst.setString(1, username);
            pst.setString(2, hashB64);
            pst.setString(3, saltB64);
            pst.setInt(4, ITERATIONS);
            pst.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false;
        }
    }

    private String hashPassword(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] key = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(key);
    }
}

