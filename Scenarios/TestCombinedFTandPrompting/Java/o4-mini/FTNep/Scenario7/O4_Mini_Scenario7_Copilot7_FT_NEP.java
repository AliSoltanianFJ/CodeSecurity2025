import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class O4_Mini_Scenario7_Copilot7_FT_NEP extends HttpServlet {
    // PBKDF2 parameters
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int    SALT_LENGTH      = 16;      // bytes
    private static final int    ITERATION_COUNT  = 100_000;
    private static final int    KEY_LENGTH       = 256;     // bits

    // Database connection parameters
    private static final String JDBC_URL      = "jdbc:mysql://localhost:3306/yourdb?useSSL=true&requireSSL=true";
    private static final String DB_USERNAME   = "dbuser";
    private static final String DB_PASSWORD   = "dbpassword";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1) Read and validate parameters
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password are required.");
            return;
        }
        username = username.trim();
        if (username.length() < 3 || username.length() > 30
                || !username.matches("^[A-Za-z0-9_]+$")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username must be 3–30 characters, letters/numbers/underscores only.");
            return;
        }
        if (password.length() < 8 || password.length() > 128) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Password must be 8–128 characters.");
            return;
        }

        // 2) Generate a random salt
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom rnd = new SecureRandom();
        rnd.nextBytes(salt);

        // 3) Derive the hash
        byte[] hashedPassword;
        try {
            KeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    ITERATION_COUNT,
                    KEY_LENGTH
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            hashedPassword = skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Failed to hash password.");
            return;
        }

        // 4) Encode salt & hash for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hashedPassword);

        // 5) Insert into database
        String insertSql = "INSERT INTO users (username, password_hash, salt, created_at) "
                         + "VALUES (?, ?, ?, NOW())";

        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USERNAME, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {

            stmt.setString(1, username);
            stmt.setString(2, hashB64);
            stmt.setString(3, saltB64);

            int rows = stmt.executeUpdate();
            if (rows == 1) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("User registered successfully.");
            } else {
                // should never happen
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Registration failed.");
            }

        } catch (SQLIntegrityConstraintViolationException dup) {
            // username unique constraint violated
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("Username already exists.");
        } catch (SQLException sqle) {
            // generic database error
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database error. Please try again later.");
            // log(sqle) to server logs in real life
        }
    }
}

