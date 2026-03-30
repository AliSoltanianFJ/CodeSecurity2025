import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario7_Copilot3_Idea3 extends HttpServlet {
    // You can also read these from web.xml or environment
    private static final String DB_URL      = System.getenv("DB_URL");
    private static final String DB_USER     = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_LENGTH     = 16;        // 128 bits
    private static final int ITERATIONS      = 65536;
    private static final int KEY_LENGTH      = 256;       // 256 bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1) Basic parameter validation
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "Username and password must be provided");
            return;
        }
        username = username.trim();
        if (username.length() < 3 || username.length() > 50) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "Username must be between 3 and 50 characters");
            return;
        }
        if (password.length() < 8) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "Password must be at least 8 characters");
            return;
        }

        // 2) Generate salt + hash
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        byte[] hash;
        try {
            hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // this should never happen
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Password hashing failed");
            return;
        }

        // 3) Store in DB
        String insertSql =
            "INSERT INTO users (username, password_hash, salt, created_at) VALUES (?,?,?,?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {

            String hashB64 = Base64.getEncoder().encodeToString(hash);
            String saltB64 = Base64.getEncoder().encodeToString(salt);

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
        }
        catch (SQLIntegrityConstraintViolationException e) {
            // assume this was a unique‐username violation
            resp.sendError(HttpServletResponse.SC_CONFLICT,
                           "Username already exists");
        }
        catch (SQLException e) {
            e.printStackTrace(); // log the real error
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Database error");
        }
    }

    // Helper to get a JDBC connection; adjust as needed
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // PBKDF2 with HMAC-SHA256
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }
}