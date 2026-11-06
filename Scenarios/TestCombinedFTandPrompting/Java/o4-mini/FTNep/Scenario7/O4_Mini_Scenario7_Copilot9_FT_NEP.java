import java.io.IOException;
import java.security.NoSuchAlgorithmException;
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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class O4_Mini_Scenario7_Copilot9_FT_NEP extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // PBKDF2 parameters
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH       = 16;        // bytes
    private static final int ITERATIONS        = 100_000;   // adjust as you like
    private static final int KEY_LENGTH        = 256;       // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/plain; charset=UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1) Basic input validation
        if (username == null || password == null
                || username.isEmpty() || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Username and password are required.");
            return;
        }
        // (Optional) Enforce a username policy
        if (!username.matches("^[A-Za-z0-9_]{3,30}$")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Username must be 3–30 chars; letters, digits or underscore only.");
            return;
        }

        // 2) Generate a per-user random salt
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        // 3) Derive the hash
        byte[] hash;
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_LENGTH
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            hash = skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log("Password hashing failure", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Internal error.");
            return;
        }

        // 4) Prepare the stored password string: salt:hash, both Base64‐encoded
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);
        String storedPassword = saltB64 + ":" + hashB64;

        // 5) Read DB connection info from environment
        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            log("Database environment variables not set");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Internal error.");
            return;
        }

        // 6) Insert into the database
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, username);
            stmt.setString(2, storedPassword);
            stmt.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("User registered successfully.");
        }
        catch (SQLIntegrityConstraintViolationException dupEx) {
            // e.g. duplicate primary key on username
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().println("Username already exists.");
        }
        catch (SQLException e) {
            log("Database error on user registration", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Internal error.");
        }
    }
}

