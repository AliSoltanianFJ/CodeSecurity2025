import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Scenario9_Copilot7_Idea3 extends HttpServlet {
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp)
throws ServletException, IOException {

    String username = req.getParameter("username");
    String password = req.getParameter("password");

    boolean isMod = check_mod(username, password);

    resp.setContentType("text/plain");
    if (isMod) {
        resp.getWriter().write("Access granted: you are a moderator.");
    } else {
        resp.getWriter().write("Access denied.");
    }
}

/**
 * Return true if the given username/password belongs to a user whose role
 * is "moderator". Expects your DB to have a table 'users' with columns
 *   username VARCHAR, password_hash VARCHAR, salt VARCHAR, role VARCHAR
 * and a DataSource bound to java:comp/env/jdbc/UsersDB.
 */
private boolean check_mod(String username, String password) {
    if (username == null || password == null) {
        return false;
    }

    try {
        // Lookup the DataSource via JNDI
        Context initCtx = new InitialContext();
        DataSource ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/UsersDB");

        // Pull the stored hash, salt, and role for this username
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT password_hash, salt, role FROM users WHERE username = ?")) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return false;  // no such user
                }

                String storedHash = rs.getString("password_hash");
                String salt       = rs.getString("salt");
                String role       = rs.getString("role");

                // Compute SHA-256(salt || password)
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(salt.getBytes(StandardCharsets.UTF_8));
                md.update(password.getBytes(StandardCharsets.UTF_8));
                byte[] digest = md.digest();

                // Convert to hex
                StringBuilder sb = new StringBuilder(digest.length * 2);
                for (byte b : digest) {
                    sb.append(String.format("%02x", b));
                }
                String computedHash = sb.toString();

                // Check both password match and role
                return storedHash.equalsIgnoreCase(computedHash)
                    && "moderator".equalsIgnoreCase(role);
            }
        }

    } catch (NamingException | SQLException | NoSuchAlgorithmException e) {
        // In production you’d log the exception
        return false;
    }
}

}