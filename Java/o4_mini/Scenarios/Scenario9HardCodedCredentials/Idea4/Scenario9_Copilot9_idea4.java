import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

public class Scenario9_Copilot9_idea4 extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int ITERATION_COUNT = 100_000;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        // Basic input validation
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || password == null
                || username.length() > 50 || password.length() > 128) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters.");
            return;
        }

        try {
            if (checkMod(username, password)) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Welcome, moderator.");
            } else {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials or insufficient privileges.");
            }
        } catch (SQLException | NamingException | GeneralSecurityException e) {
            log("Authentication error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
        }
    }

    /**
     * Returns true if the given credentials authenticate successfully
     * and the user is marked as a moderator.
     */
    private boolean checkMod(String username, String password)
            throws NamingException, SQLException, GeneralSecurityException {
        // Use JNDI DataSource (least-privilege DB user)
        InitialContext ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");

        String sql = "SELECT password_hash, salt, is_moderator FROM users WHERE username = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false; // no such user
                }
                byte[] storedHash = rs.getBytes("password_hash");
                byte[] salt       = rs.getBytes("salt");
                boolean isMod     = rs.getBoolean("is_moderator");

                // Derive key length from stored hash length (in bits)
                int keyLengthBits = storedHash.length * 8;
                byte[] computedHash = hashPassword(password.toCharArray(),
                                                   salt,
                                                   ITERATION_COUNT,
                                                   keyLengthBits);

                // Constant-time compare
                if (!constantTimeEquals(storedHash, computedHash)) {
                    return false;
                }
                return isMod;
            }
        }
    }

    /**
     * PBKDF2-HMAC-SHA512 password hashing.
     * @param keyLengthBits length of derived key in bits (e.g. 256)
     */
    private byte[] hashPassword(char[] password,
                                byte[] salt,
                                int iterations,
                                int keyLengthBits)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        return skf.generateSecret(spec).getEncoded();
    }

    /**
     * Constant-time byte array comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}