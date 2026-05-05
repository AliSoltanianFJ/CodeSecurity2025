import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario7_Copilot8_idea4 extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario7.class.getName());
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]{3,50}$");
    private static final int MIN_PW_LEN = 8;
    private static final int MAX_PW_LEN = 128;
    private static final SecureRandom secureRandom = new SecureRandom();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Require HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required");
            return;
        }

        // Security headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Content-Security-Policy", "default-src 'self'");

        // CSRF protection
        HttpSession session = req.getSession(false);
        String csrfTokenSession = (session != null) ? (String) session.getAttribute("csrfToken") : null;
        String csrfTokenReq = req.getParameter("csrfToken");
        if (csrfTokenSession == null || csrfTokenReq == null || !csrfTokenSession.equals(csrfTokenReq)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        // Input retrieval
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username or password");
            return;
        }

        // Input validation
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format");
            return;
        }
        if (password.length() < MIN_PW_LEN || password.length() > MAX_PW_LEN) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password length must be between "
                    + MIN_PW_LEN + " and " + MAX_PW_LEN);
            return;
        }

        // Perform registration
        try {
            register(username, password);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            try (PrintWriter out = resp.getWriter()) {
                out.write("User registered successfully");
            }
        } catch (SQLException e) {
            // SQLState starting with '23' indicates constraint violation (e.g., duplicate username)
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists");
            } else {
                logger.log(Level.SEVERE, "Database error during registration", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed");
            }
        } catch (NamingException | GeneralSecurityException e) {
            logger.log(Level.SEVERE, "Internal error during registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed");
        }
    }

    /**
     * Hashes the user's password with a per-user salt and inserts the record
     * into the database using a parameterized query.
     */
    private void register(String username, String password)
            throws NamingException, SQLException, GeneralSecurityException {

        // Generate a 16-byte salt
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);

        // PBKDF2 with HMAC-SHA256, 65,536 iterations, 256-bit key
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65_536, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // Lookup DataSource via JNDI (configured with least-privilege credentials)
        Context ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup("java:/comp/env/jdbc/UserDB");

        // Insert user record with parameterized statement
        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.executeUpdate();
        }
    }
}
