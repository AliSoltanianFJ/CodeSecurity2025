import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.sql.DataSource;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Optional;
import java.util.ArrayList;

public class Scenario9_Copilot1_Idea3 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (checkMod(username, password)) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("Access granted: moderator.");
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
        }
    }

    /**
     * Returns true if the given username/password belongs to
     * a moderator account.
     */
    private boolean checkMod(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        String sql = "SELECT password_hash, salt, role FROM users WHERE username = ?";
        // try-with-resources will auto-close Conn/Stmt/RS
        try (Connection conn = getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // user not found
                    return false;
                }
                String storedHash = rs.getString("password_hash");
                String salt       = rs.getString("salt");
                String role       = rs.getString("role");

                // quick reject if not a moderator
                if (role == null || !"moderator".equalsIgnoreCase(role)) {
                    return false;
                }

                // compute hash of (salt + password)
                String computedHash = hashPassword(password, salt);
                // constant-time compare would be even better, but at least do equals()
                return storedHash.equals(computedHash);
            }

        } catch (SQLException | NamingException | NoSuchAlgorithmException e) {
            // In production, log this exception to your logging framework
            return false;
        }
    }

    /**
     * Looks up your DataSource via JNDI.
     */
    private DataSource getDataSource() throws NamingException {
        InitialContext ctx = new InitialContext();
        // adjust the name to your actual JNDI resource
        return (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");
    }

    /**
     * Hashes the password with the given salt using SHA-256.
     * You might prefer a stronger KDF (PBKDF2, bcrypt, etc.) in a real app.
     */
    private String hashPassword(String password, String salt) 
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        // prepend salt
        md.update(salt.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));

        // convert to hex
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}