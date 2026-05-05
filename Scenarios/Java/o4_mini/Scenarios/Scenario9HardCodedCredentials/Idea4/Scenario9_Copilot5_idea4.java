import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
import javax.sql.DataSource;

public class Scenario9_Copilot5_idea4 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario9.class.getName());

    // PBKDF2 parameters
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int HASH_ITERATIONS = 100_000;
    private static final int HASH_KEY_LENGTH = 256; // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Basic input validation: non-null, reasonable length, allowed chars
        if (username == null || password == null ||
            username.length() < 3 || username.length() > 50 ||
            password.length() < 8 || password.length() > 128 ||
            !username.matches("^[A-Za-z0-9._-]+$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials.");
            return;
        }

        boolean isModerator = false;
        try {
            isModerator = checkMod(username, password);
        } catch (Exception e) {
            // Log internals; do not leak to client
            LOGGER.log(Level.SEVERE, "Error during authentication", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            return;
        }

        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("Welcome, moderator!");
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
        }
    }

    /**
     * Verifies whether the given username/password maps to a moderator account.
     * Uses PBKDF2WithHmacSHA256 for hashing, with per-user salt stored in DB.
     * All SQL uses prepared statements to prevent injection.
     */
    private boolean checkMod(String username, String password)
            throws NamingException, SQLException, NoSuchAlgorithmException, InvalidKeySpecException {

        // Lookup DataSource via JNDI (configured in the app server, not in code)
        InitialContext ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/YourAppDS");

        String sql = "SELECT password_hash, salt FROM moderators WHERE username = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // No such user
                    return false;
                }
                byte[] storedHash = rs.getBytes("password_hash");
                byte[] salt = rs.getBytes("salt");

                // Derive hash from provided password
                PBEKeySpec spec = new PBEKeySpec(
                        password.toCharArray(),
                        salt,
                        HASH_ITERATIONS,
                        HASH_KEY_LENGTH
                );
                SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
                byte[] computedHash = skf.generateSecret(spec).getEncoded();

                // Constant-time comparison to prevent timing attacks
                return java.security.MessageDigest.isEqual(storedHash, computedHash);
            }
        }
    }
}
