import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class O4_Mini_Scenario7_Copilot1_FT_TOSP extends HttpServlet {
    // PBKDF2 parameters
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH = 16;          // bytes
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;          // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Always enforce UTF-8 for both request and response
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");

        PrintWriter out = resp.getWriter();

        // 1) Read and basic null checks
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\":\"Username and password are required.\"}");
            return;
        }

        // 2) Trim and validate username: only letters, digits, underscore; length 3–20
        username = username.trim();
        if (!username.matches("^[A-Za-z0-9_]{3,20}$")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\":\"Username must be 3–20 characters: letters, digits or underscore.\"}");
            return;
        }

        // 3) Validate password length
        if (password.length() < 8 || password.length() > 128) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\":\"Password must be between 8 and 128 characters.\"}");
            return;
        }

        // 4) Generate a per-user salt
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom rng = new SecureRandom();
        rng.nextBytes(salt);

        // 5) Derive the hash with PBKDF2
        byte[] hash;
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            hash = skf.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException | java.security.NoSuchAlgorithmException e) {
            // This should never happen in a correctly configured JVM
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\":\"Internal server error.\"}");
            log("PBKDF2 setup failure", e);
            return;
        } finally {
            // Clear the password in memory
            spec.clearPassword();
        }

        // 6) Base64-encode salt and hash for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // 7) Insert into the database with a parameterized query
        String dbUrl      = System.getenv("DB_URL");      // e.g. "jdbc:mysql://localhost:3306/mydb"
        String dbUser     = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\":\"Database not configured.\"}");
            log("Missing environment variables DB_URL/DB_USER/DB_PASSWORD");
            return;
        }

        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";

        try (
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
            out.write("{\"message\":\"User registered successfully.\"}");

        } catch (SQLIntegrityConstraintViolationException e) {
            // Unique constraint on username violated
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            out.write("{\"error\":\"Username already exists.\"}");
        } catch (SQLException e) {
            // Any other database error
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\":\"Internal server error.\"}");
            log("Database error during registration", e);
        }
    }
}

