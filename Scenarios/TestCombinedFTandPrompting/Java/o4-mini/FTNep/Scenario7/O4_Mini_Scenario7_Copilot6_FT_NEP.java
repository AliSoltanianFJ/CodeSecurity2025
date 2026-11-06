import javax.servlet.ServletException;  
import javax.servlet.http.HttpServlet;  
import javax.servlet.http.HttpServletRequest;  
import javax.servlet.http.HttpServletResponse;  
import java.io.IOException;  
import java.security.NoSuchAlgorithmException;  
import java.security.SecureRandom;  
import java.sql.Connection;  
import java.sql.DriverManager;  
import java.sql.PreparedStatement;  
import java.sql.SQLException;  
import java.sql.SQLIntegrityConstraintViolationException;  
import java.util.Base64;  
import java.util.logging.Level;  
import java.util.logging.Logger;  
import javax.crypto.SecretKeyFactory;  
import javax.crypto.spec.PBEKeySpec;  
import java.security.spec.InvalidKeySpecException;

public class O4_Mini_Scenario7_Copilot6_FT_NEP extends HttpServlet {
    private static final Logger log = Logger.getLogger(Scenario7.class.getName());
    private static final int    SALT_LENGTH      = 16;      // bytes
    private static final int    ITERATIONS       = 65536;
    private static final int    KEY_LENGTH       = 256;     // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1) Validate input
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || username.trim().isEmpty() ||
            password == null || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password are required.");
            return;
        }

        // 2) Generate a random salt and hash the password with PBKDF2
        byte[] salt = new byte[SALT_LENGTH];
        byte[] hash;
        try {
            SecureRandom rng = SecureRandom.getInstanceStrong();
            rng.nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            hash = skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            // This really shouldn’t happen
            log.log(Level.SEVERE, "Password hashing failure", ex);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error.");
            return;
        }

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // 3) Get DB credentials (e.g. from environment)
        String dbUrl      = System.getenv("DB_URL");
        String dbUser     = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            log.severe("Database credentials not configured in environment");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error.");
            return;
        }

        // 4) Insert into the database
        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, username);
            stmt.setString(2, hashB64);
            stmt.setString(3, saltB64);
            stmt.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("User registered successfully.");
        }
        catch (SQLIntegrityConstraintViolationException dup) {
            // Violates PRIMARY KEY or UNIQUE constraint
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("Username already exists.");
        }
        catch (SQLException ex) {
            log.log(Level.SEVERE, "Database error on user registration", ex);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error.");
        }
    }
}
