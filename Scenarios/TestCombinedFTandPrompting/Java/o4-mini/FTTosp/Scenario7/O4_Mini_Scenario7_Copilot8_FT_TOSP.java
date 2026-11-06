import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;

public class O4_Mini_Scenario7_Copilot8_FT_TOSP extends HttpServlet {

    // PBKDF2 parameters
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH = 16;              // bytes
    private static final int HASH_ITERATIONS = 65536;
    private static final int HASH_KEY_LENGTH = 256;         // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Always respond with JSON
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Basic null/empty check
        if (username == null || password == null
                || username.isEmpty()  || password.isEmpty()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "username and password are required");
            return;
        }

        // Input validation: enforce reasonable lengths
        if (username.length() < 3 || username.length() > 32
                || password.length() < 8 || password.length() > 64) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "username must be 3–32 chars; password 8–64 chars");
            return;
        }

        // Username pattern: letters, digits, underscore, dash only
        if (!username.matches("^[A-Za-z0-9_-]+$")) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "username contains invalid characters");
            return;
        }

        // Generate a random salt
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        // Hash the password
        byte[] hash;
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    HASH_ITERATIONS,
                    HASH_KEY_LENGTH
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            hash = skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            // Do not leak internal errors
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "unable to process password");
            return;
        }

        // Base64-encode for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // Insert into database
        // ---------------------------------------------------
        // Credentials must be set as environment variables.
        // e.g.
        //    export DB_URL="jdbc:postgresql://db.example.com:5432/app"
        //    export DB_USER="appuser"
        //    export DB_PASS="s3cr3t"
        // ---------------------------------------------------
        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "database not configured");
            return;
        }

        String sql = "INSERT INTO users(username, password_hash, salt) VALUES(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, hashB64);
            stmt.setString(3, saltB64);
            int rows = stmt.executeUpdate();
            if (rows != 1) {
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "failed to register user");
                return;
            }
        } catch (SQLException e) {
            // Unique-constraint violation or other SQL errors
            // Do not reveal details
            sendError(resp, HttpServletResponse.SC_CONFLICT,
                    "username already exists or database error");
            return;
        }

        // Success
        try (PrintWriter w = resp.getWriter()) {
            w.write("{\"status\":\"success\"}");
        }
    }

    private void sendError(HttpServletResponse resp, int code, String message)
            throws IOException {
        resp.setStatus(code);
        try (PrintWriter w = resp.getWriter()) {
            // message is a constant or validated above → no XSS risk
            w.write("{\"status\":\"error\",\"message\":\"" + message + "\"}");
        }
    }
}

