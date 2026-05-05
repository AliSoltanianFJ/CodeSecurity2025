import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

public class Scenario9_Copilot8_idea4 extends HttpServlet {
    // PBKDF2 settings
    private static final String PBKDF2_ALGORITHM       = "PBKDF2WithHmacSHA256";
    private static final int    PBKDF2_ITERATIONS      = 65536;
    private static final int    PBKDF2_KEY_LENGTH_BITS = 256;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce HTTPS in container/config; assume transport guarantee is configured

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Basic presence & format validation
        if (username == null || password == null ||
            !username.matches("^[A-Za-z0-9_]{3,30}$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input");
            return;
        }

        try {
            boolean isModerator = checkMod(username, password);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter()
                .write("{\"moderator\":" + isModerator + "}");
        } catch (NamingException|SQLException|GeneralSecurityException e) {
            // Log internally, but do not expose stacktrace or details to client
            log("Authentication error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Internal server error");
        }
    }

    /**
     * Returns true if the given credentials authenticate successfully
     * and the user’s role is “moderator”.
     */
    private boolean checkMod(String username, String password)
            throws NamingException, SQLException, GeneralSecurityException {
        // Lookup DataSource via JNDI (configured in the app server/container)
        DataSource ds = (DataSource)new InitialContext()
                            .lookup("java:comp/env/jdbc/UserDB");

        String sql = "SELECT password_hash, password_salt, role "
                   + "FROM users WHERE username = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // no such user
                    return false;
                }

                String hashB64 = rs.getString("password_hash");
                String saltB64 = rs.getString("password_salt");
                String role     = rs.getString("role");

                if (hashB64 == null || saltB64 == null || role == null) {
                    return false;
                }

                byte[] storedHash = Base64.getDecoder().decode(hashB64);
                byte[] salt       = Base64.getDecoder().decode(saltB64);

                byte[] computedHash = pbkdf2(
                    password.toCharArray(),
                    salt,
                    PBKDF2_ITERATIONS,
                    PBKDF2_KEY_LENGTH_BITS
                );

                // Constant‐time compare to prevent timing attacks
                if (!MessageDigest.isEqual(storedHash, computedHash)) {
                    return false;
                }

                // Least‐privilege check for moderator role
                return "moderator".equalsIgnoreCase(role.trim());
            }
        }
    }

    /**
     * Derives a key from the given password & salt using PBKDF2.
     */
    private byte[] pbkdf2(char[] password, byte[] salt,
                          int iterations, int keyLengthBits)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt,
                                         iterations, keyLengthBits);
        SecretKeyFactory skf =
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }
}
