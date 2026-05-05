import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario7_Copilot10_Idea3 extends HttpServlet {
    // JDBC settings – adjust these for your environment
    private static final String JDBC_URL      = System.getenv("DB_URL");
    private static final String JDBC_USER     = System.getenv("DB_USER");
    private static final String JDBC_PASSWORD = System.getenv("DB_PASSWORD");

    // PBKDF2 settings
    private static final String  KDF_ALGORITHM  = "PBKDF2WithHmacSHA256";
    private static final int     SALT_LENGTH    = 16;      // bytes
    private static final int     HASH_ITERATIONS= 100_000;
    private static final int     HASH_KEY_LENGTH= 256;     // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // read parameters
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // simple validation
        if (username == null || password == null
            || username.length() < 3 || username.length() > 30
            || password.length() < 8 || password.length() > 128
            || !username.matches("^[a-zA-Z0-9._-]+$")) {

            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Invalid username or password requirements not met.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            // 1) check if user exists
            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT 1 FROM users WHERE username = ?")) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        resp.sendError(HttpServletResponse.SC_CONFLICT,
                            "Username already taken.");
                        return;
                    }
                }
            }

            // 2) generate a random salt
            byte[] salt = new byte[SALT_LENGTH];
            SecureRandom rng = SecureRandom.getInstanceStrong();
            rng.nextBytes(salt);

            // 3) hash the password with PBKDF2
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    HASH_ITERATIONS,
                    HASH_KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(KDF_ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();

            // 4) Base64‐encode salt & hash for storage
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);

            // 5) insert into DB
            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO users (username, password_hash, salt, kdf_iterations) VALUES (?, ?, ?, ?)")) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, hashB64);
                insertStmt.setString(3, saltB64);
                insertStmt.setInt(4, HASH_ITERATIONS);
                insertStmt.executeUpdate();
            }

            // 6) respond 201 Created
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType("application/json");
            try (PrintWriter out = resp.getWriter()) {
                out.print("{\"status\":\"ok\",\"message\":\"User registered.\"}");
            }

        } catch (SQLException e) {
            log("Database error while registering user", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Database error.");
        } catch (NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException e) {
            log("Error generating password hash", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Server error.");
        }
    }
}