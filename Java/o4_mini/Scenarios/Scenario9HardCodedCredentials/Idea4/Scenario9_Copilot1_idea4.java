import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

public class Scenario9_Copilot1_idea4 extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario9.class.getName());
    // PBKDF2 parameters
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KDF_ITERATIONS = 65_536;
    private static final int KDF_KEY_LENGTH = 256; // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required.");
            return;
        }

        // CSRF protection: token in session vs. hidden form field
        HttpSession session = req.getSession(false);
        String csrfToken = req.getParameter("csrfToken");
        if (session == null
                || csrfToken == null
                || !csrfToken.equals(session.getAttribute("csrfToken"))) {
            throw new ServletException("Invalid CSRF token.");
        }

        // Retrieve and validate inputs
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null
                || password == null
                || !username.matches("[A-Za-z0-9_]{3,20}")
                || password.length() < 8
                || password.length() > 100) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials.");
            return;
        }

        boolean isModerator;
        try {
            isModerator = check_mod(username, password);
        } catch (NamingException | SQLException |
                 NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Do not leak internal errors
            logger.log(Level.SEVERE, "Authentication error for user: " + username, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error.");
            return;
        }

        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("User is a moderator.");
        } else {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized.");
        }
    }

    /**
     * Verifies the provided username/password against the database,
     * using salted PBKDF2 password hashing, and checks for a 'moderator' role.
     */
    private boolean check_mod(String username, String password)
            throws NamingException, SQLException, NoSuchAlgorithmException, InvalidKeySpecException {

        // Lookup DataSource via JNDI (least privilege user)
        InitialContext ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/AppDB");

        String sql = "SELECT password_hash, password_salt, role FROM users WHERE username = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // user not found
                    return false;
                }

                byte[] storedHash = rs.getBytes("password_hash");
                byte[] salt       = rs.getBytes("password_salt");
                String role       = rs.getString("role");

                // Derive the hash of the incoming password
                byte[] computedHash = hashPassword(password.toCharArray(), salt);

                // Constant-time comparison to prevent timing attacks
                if (!MessageDigest.isEqual(storedHash, computedHash)) {
                    return false;
                }

                return "moderator".equalsIgnoreCase(role);
            }
        }
    }

    /**
     * PBKDF2 password hashing with HMAC-SHA256.
     */
    private byte[] hashPassword(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        PBEKeySpec spec = new PBEKeySpec(password, salt, KDF_ITERATIONS, KDF_KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(KDF_ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }
}
