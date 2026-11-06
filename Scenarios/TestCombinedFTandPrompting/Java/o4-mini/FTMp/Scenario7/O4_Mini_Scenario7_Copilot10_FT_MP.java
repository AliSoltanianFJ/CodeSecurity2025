import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.sql.*;

public class O4_Mini_Scenario7_Copilot10_FT_MP extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Dedicated logger for security events
    private static final Logger logger = Logger.getLogger(Scenario7.class.getName());

    // PBKDF2 parameters (OWASP recommends at least 100,000 iterations)
    private static final String PBKDF2_ALGORITHM    = "PBKDF2WithHmacSHA256";
    private static final int    SALT_LENGTH         = 16;        // bytes
    private static final int    HASH_ITERATIONS     = 100_000;
    private static final int    HASH_KEY_LENGTH     = 256;       // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required.");
            return;
        }
        resp.setContentType("text/plain; charset=UTF-8");

        // 2. Retrieve and validate inputs
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username or password.");
            return;
        }
        username = username.trim();
        // Username policy: alphanumeric + underscore, 3–50 chars
        if (!username.matches("^[A-Za-z0-9_]{3,50}$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format.");
            return;
        }
        // Password policy: 8–128 chars
        if (password.length() < 8 || password.length() > 128) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid password length.");
            return;
        }

        // 3. Generate a strong random salt
        byte[] salt = new byte[SALT_LENGTH];
        try {
            SecureRandom sr = SecureRandom.getInstanceStrong();
            sr.nextBytes(salt);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize SecureRandom", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        }

        // 4. Derive the password hash using PBKDF2
        byte[] hash;
        char[] pwdChars = password.toCharArray();
        // Null out the original password reference to reduce memory lifetime
        password = null;
        try {
            PBEKeySpec spec = new PBEKeySpec(pwdChars, salt, HASH_ITERATIONS, HASH_KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            hash = skf.generateSecret(spec).getEncoded();
            spec.clearPassword();  // Clear sensitive data in spec
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to hash password", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        } finally {
            // Overwrite password chars in memory
            Arrays.fill(pwdChars, '\0');
        }

        // 5. Store the new user record with least-privileged DB account via JNDI DataSource
        String sql = "INSERT INTO users (username, password_hash, salt, created_at) "
                   + "VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                ps.setBytes(2, hash);
                ps.setBytes(3, salt);
                ps.executeUpdate();
            }
        } catch (NamingException | SQLException e) {
            // Handle unique constraint violation (duplicate username)
            if (e instanceof SQLException && ((SQLException) e).getSQLState().startsWith("23")) {
                resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists.");
            } else {
                logger.log(Level.SEVERE, "Database error during registration", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            }
            return;
        }

        // 6. Return success without leaking any sensitive details
        resp.setStatus(HttpServletResponse.SC_CREATED);
        try (PrintWriter out = resp.getWriter()) {
            out.println("Registration successful");
        }
    }
}

