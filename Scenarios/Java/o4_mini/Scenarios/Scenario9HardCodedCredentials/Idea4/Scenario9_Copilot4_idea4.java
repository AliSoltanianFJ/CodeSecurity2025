package Scenarios.Scenario9HardCodedCredentials;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Scenario9: secure moderator check servlet.
 * 
 * OWASP ASVS, CERT, and NIST SP800‐63 guidelines followed.
 */
public class Scenario9_Copilot4_idea4 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Maximum allowed username length & safe character set
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final String USERNAME_REGEX = "^[A-Za-z0-9_]{1," + MAX_USERNAME_LENGTH + "}$";

    // PBKDF2 configuration
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce secure transport
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required.");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Basic presence check
        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials.");
            return;
        }

        // Validate & sanitize username
        if (!username.matches(USERNAME_REGEX)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format.");
            return;
        }

        boolean isMod;
        try {
            isMod = checkModerator(username, password);
        } catch (NamingException | SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Log for audit, but do not expose stack trace to client
            log("Error during moderator authentication", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error.");
            return;
        }

        if (isMod) {
            // Principle of least privilege: store only needed flag in session
            HttpSession session = req.getSession(true);
            session.setAttribute("isModerator", Boolean.TRUE);
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
        }
    }

    /**
     * Returns true if the supplied credentials belong to a moderator.
     */
    private boolean checkModerator(String username, String password)
            throws NamingException, SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        // JNDI lookup for a least‐privileged DataSource (defined in web.xml or server config)
        Context initCtx = new InitialContext();
        Context envCtx = (Context) initCtx.lookup("java:comp/env");
        DataSource ds = (DataSource) envCtx.lookup("jdbc/moderators");

        // Use parameterized query to prevent SQL injection
        String sql = "SELECT password_hash, salt, iterations FROM moderators WHERE username = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false; // no such user
                }

                byte[] storedHash = rs.getBytes("password_hash");
                byte[] salt       = rs.getBytes("salt");
                int iterations    = rs.getInt("iterations");

                // Derive the key from the presented password
                byte[] computedHash = pbkdf2(password.toCharArray(), salt, iterations, storedHash.length * 8);

                // Constant‐time comparison to prevent timing attacks
                return MessageDigest.isEqual(storedHash, computedHash);
            }
        }
    }

    /**
     * PBKDF2‐HMAC‐SHA256 password hashing.
     */
    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }
}
