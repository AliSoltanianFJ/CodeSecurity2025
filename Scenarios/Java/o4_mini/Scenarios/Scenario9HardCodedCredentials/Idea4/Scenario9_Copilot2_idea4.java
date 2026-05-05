import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
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

/**
 * Scenario9 servlet: secure moderator check.
 * Security decisions:
 *  - Enforce HTTPS
 *  - Strict input validation & sanitization
 *  - Use JNDI DataSource (least‐privilege DB user)
 *  - Parameterized queries to prevent SQL injection
 *  - PBKDF2WithHmacSHA256 with high iteration count & per‐user salt
 *  - Constant‐time hash comparison
 *  - Generic error responses (no sensitive info leaked)
 *  - Secure response headers & UTF‐8 JSON output
 */
public class Scenario9_Copilot2_idea4 extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario9.class.getName());
    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256; // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce secure transport
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        // Security headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setContentType("application/json;charset=UTF-8");

        // Basic input validation & sanitization
        String username = sanitizeUsername(req.getParameter("username"));
        String password = req.getParameter("password");
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "{\"error\":\"Invalid credentials.\"}");
            return;
        }

        boolean isModerator;
        try {
            isModerator = checkMod(username, password);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during authentication check", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "{\"error\":\"Internal error.\"}");
            return;
        }

        // Return JSON boolean response
        try (PrintWriter out = resp.getWriter()) {
            out.write("{\"isModerator\":" + isModerator + "}");
        }
    }

    /**
     * Strictly sanitize username: allow only a safe subset of chars.
     */
    private String sanitizeUsername(String input) {
        if (input == null) {
            return null;
        }
        // Only letters, digits, underscore, dot, hyphen, at‐sign
        return input.replaceAll("[^a-zA-Z0-9@._-]", "");
    }

    /**
     * Verify credentials and role.
     * @return true if credentials valid AND role == "moderator"
     */
    private boolean checkMod(String username, String password)
            throws NamingException, SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        // SQL uses parameterized query to prevent injection
        String sql = "SELECT password_hash, salt, role FROM users WHERE username = ?";
        // Lookup pooled DataSource; configured with least‐privilege account
        DataSource ds = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/UserDB");
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // user not found
                    return false;
                }
                byte[] storedHash = rs.getBytes("password_hash");
                byte[] salt       = rs.getBytes("salt");
                String role       = rs.getString("role");

                // Derive hash from supplied password using same salt & parameters
                byte[] computedHash = pbkdf2(password.toCharArray(), salt);

                // Constant‐time comparison to prevent timing attacks
                if (!MessageDigest.isEqual(storedHash, computedHash)) {
                    return false;
                }

                // Enforce role check
                return "moderator".equalsIgnoreCase(role);
            }
        }
    }

    /**
     * PBKDF2 password hashing with user‐specific salt.
     */
    private byte[] pbkdf2(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }
}
