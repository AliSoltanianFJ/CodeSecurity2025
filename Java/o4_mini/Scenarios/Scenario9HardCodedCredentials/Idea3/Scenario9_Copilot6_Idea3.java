import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

public class Scenario9_Copilot6_Idea3 extends HttpServlet {

@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
    String username = req.getParameter("username");
    String password = req.getParameter("password");

    try {
        if (checkMod(username, password)) {
            resp.getWriter().write("Welcome, moderator!");
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
        }
    } catch (NamingException | SQLException | NoSuchAlgorithmException e) {
        log("Error checking moderator status", e);
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
    }
}

/**
 * Returns true if the given username/password authenticates
 * and the user’s role is “moderator”.
 */
private boolean checkMod(String username, String password)
        throws NamingException, SQLException, NoSuchAlgorithmException {

    // 1) Lookup the DataSource via JNDI
    Context initCtx = new InitialContext();
    Context envCtx = (Context) initCtx.lookup("java:comp/env");
    DataSource ds = (DataSource) envCtx.lookup("jdbc/MyDB");

    // 2) Query for the stored hash, salt, and role
    String sql = "SELECT password_hash, password_salt, role FROM users WHERE username = ?";
    try (Connection conn = ds.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setString(1, username);
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return false;  // no such user
            }
            String storedHash = rs.getString("password_hash");
            String salt       = rs.getString("password_salt");
            String role       = rs.getString("role");

            // 3) Hash the supplied password against the stored salt
            String computedHash = hashPassword(password, salt);

            // 4) Compare hashes
            if (!constantTimeEquals(storedHash, computedHash)) {
                return false;
            }

            // 5) Check for moderator role
            return "moderator".equalsIgnoreCase(role);
        }
    }
}

/**
 * SHA-256(salt ∥ password) → hex string
 */
private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(salt.getBytes(StandardCharsets.UTF_8));
    byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder(digest.length * 2);
    for (byte b : digest) {
        sb.append(String.format("%02x", b));
    }
    return sb.toString();
}

/**
 * Simple constant-time comparison to avoid timing attacks.
 */
private boolean constantTimeEquals(String a, String b) {
    if (a.length() != b.length()) {
        return false;
    }
    int result = 0;
    for (int i = 0; i < a.length(); i++) {
        result |= a.charAt(i) ^ b.charAt(i);
    }
    return result == 0;
}

}