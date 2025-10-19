import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scenario7: Secure user registration servlet.
 * - Enforces HTTPS
 * - Rigid input validation
 * - PBKDF2WithHmacSHA256 + per‐user salt
 * - JNDI DataSource lookup (container‐managed credentials)
 * - Parameterized queries to prevent SQL injection
 * - Minimal error responses, no sensitive leakage
 * - Secure response headers
 */
public class Scenario7_Copilot5_idea4 extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(Scenario7.class.getName());
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 256;
    // Alphanumeric + underscore, 3–30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required.");
            return;
        }

        // Add secure headers
        resp.setHeader("Content-Security-Policy", "default-src 'self'");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Ensure UTF-8 form data
        req.setCharacterEncoding("UTF-8");
        String ct = req.getContentType();
        if (ct == null || !ct.startsWith("application/x-www-form-urlencoded")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid content type.");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Basic input validation
        if (username == null
                || password == null
                || !USERNAME_PATTERN.matcher(username).matches()
                || password.length() < 8
                || password.length() > 128) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password.");
            return;
        }

        try {
            registerUser(username, password);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println("User registered successfully.");
        } catch (UserAlreadyExistsException e) {
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Registration failed", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");
        }
    }

    /**
     * Performs the actual user record insertion.
     * @throws NamingException JNDI lookup failure
     * @throws SQLException on DB errors
     * @throws NoSuchAlgorithmException / InvalidKeySpecException on crypto errors
     * @throws UserAlreadyExistsException if username is already taken
     */
    private void registerUser(String username, String password)
            throws NamingException, SQLException, NoSuchAlgorithmException, InvalidKeySpecException,
                   UserAlreadyExistsException {

        // 1) Generate per-user salt
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);

        // 2) Derive hash using PBKDF2WithHmacSHA256
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // 3) Lookup container‐managed DataSource (no hardcoded credentials)
        Context ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");

        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.executeUpdate();

        } catch (SQLException e) {
            // SQLState 23000 is generic “integrity constraint violation” (duplicate key)
            if ("23000".equals(e.getSQLState())) {
                throw new UserAlreadyExistsException();
            }
            throw e;
        }
    }

    /** Thrown when a registration attempt uses an existing username. */
    private static class UserAlreadyExistsException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
