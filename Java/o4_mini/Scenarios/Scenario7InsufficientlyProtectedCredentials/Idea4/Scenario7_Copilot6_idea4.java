import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.Base64;

public class Scenario7_Copilot6_idea4 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // PBKDF2 parameters
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256;

    private DataSource dataSource;

    @Override
    public void init() throws ServletException {
        // Lookup a JNDI DataSource for least-privilege DB access
        try {
            InitialContext ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDB");
        } catch (NamingException ne) {
            throw new ServletException("Unable to initialize DataSource", ne);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce TLS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Insecure channel");
            return;
        }

        // CSRF token check
        HttpSession session = req.getSession(false);
        String csrf = req.getParameter("csrfToken");
        if (session == null
                || csrf == null
                || !csrf.equals(session.getAttribute("csrfToken"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        // Input validation (OWASP recommended patterns)
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null
                || password == null
                || !username.matches("^[A-Za-z0-9_\\-]{5,20}$")     // allow letters, digits, underscore, hyphen
                || password.length() < 8
                || password.length() > 64) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials format");
            return;
        }

        // Generate per-user salt
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);

        // Hash the password
        byte[] hash = hashPassword(password.toCharArray(), salt);

        // Store base64-encoded salt+hash
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // Insert user record using a prepared statement
        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
        }
        catch (SQLIntegrityConstraintViolationException dup) {
            // Unique username violation
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists");
        }
        catch (SQLException sqle) {
            log("Database error during registration", sqle);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    /**
     * Hashes a password with PBKDF2-HMAC-SHA256.
     * See OWASP Password Storage Guidance.
     */
    private static byte[] hashPassword(char[] password, byte[] salt) throws ServletException {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ServletException("Error hashing password", e);
        }
    }
}
