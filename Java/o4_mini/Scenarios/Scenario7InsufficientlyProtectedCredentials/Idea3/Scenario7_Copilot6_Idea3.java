import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Arrays;
import java.util.Base64;

public class Scenario7_Copilot6_Idea3 extends HttpServlet {
    private static final int SALT_LENGTH = 16;          // 16 bytes = 128 bits
    private static final int ITERATIONS  = 65536;       // work factor
    private static final int KEY_LENGTH  = 256;         // derived key length in bits
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || username.isEmpty()
         || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "username and password must be provided");
            return;
        }

        try {
            boolean created = register(username, password.toCharArray());
            if (created) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("User registered successfully");
            } else {
                resp.sendError(HttpServletResponse.SC_CONFLICT,
                               "Username already exists");
            }
        } catch (Exception e) {
            log("Error during registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Internal server error");
        }
    }

    /**
     * @param username user's login name
     * @param password user's raw password (char[])
     * @return true if newly created; false if username already existed
     */
    private boolean register(String username, char[] password) throws Exception {
        // 1) Read DB config from environment
        String url    = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (url == null || dbUser == null || dbPass == null) {
            throw new IllegalStateException(
                "DB_URL, DB_USER and DB_PASS must be set as environment variables");
        }

        // 2) Generate a random salt
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);

        // 3) Derive the hash
        byte[] hash = pbkdf2(password, salt, ITERATIONS, KEY_LENGTH);

        // 4) Zero out the clear‐text password in memory
        Arrays.fill(password, '\0');

        // 5) Base64‐encode salt and hash for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // 6) Insert into database
        //    Make sure your JDBC driver is on the classpath; e.g. MySQL's
        //    com.mysql.cj.jdbc.Driver if using MySQL.
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass)) {

            // 6a) Check for existing username
            String checkSql = "SELECT 1 FROM users WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return false;  // user already exists
                    }
                }
            }

            // 6b) Insert new user
            String insertSql =
                "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, username);
                ps.setString(2, hashB64);
                ps.setString(3, saltB64);
                ps.executeUpdate();
            }

            return true;
        }
    }

    /**
     * PBKDF2 with HMAC‐SHA256
     */
    private static byte[] pbkdf2(char[] password, byte[] salt,
                                 int iterations, int keyLength) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory
                                  .getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }
}