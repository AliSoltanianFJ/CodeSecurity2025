// ...existing code...
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot2_idea1 extends HttpServlet {
    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || username.isEmpty() || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing credentials");
            return;
        }

        boolean isMod;
        try {
            isMod = check_mod(username, password);
        } catch (IllegalStateException e) {
            // configuration problem
            throw new ServletException("Server configuration error", e);
        } catch (SQLException | NoSuchAlgorithmException e) {
            // database / crypto errors
            throw new ServletException("Error checking moderator status", e);
        }

        resp.setContentType("text/plain; charset=UTF-8");
        if (isMod) {
            resp.getWriter().write("Moderator access granted");
        } else {
            resp.getWriter().write("Access denied");
        }
    }

    /**
     * Verify credentials against the users table and return true if the account has the moderator role.
     * Uses environment variables for database connection:
     *   DB_URL   - JDBC URL (required)
     *   DB_USER  - DB user (required)
     *   DB_PASS  - DB password (required)
     * Optional:
     *   MODERATOR_ROLE - role name to compare against (defaults to "moderator")
     *
     * This method hashes the supplied password with SHA-256 and compares it to the stored password_hash field.
     */
    private boolean check_mod(String username, String password)
            throws SQLException, NoSuchAlgorithmException {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        String moderatorRole = System.getenv("MODERATOR_ROLE");
        if (moderatorRole == null || moderatorRole.isEmpty()) {
            moderatorRole = "moderator";
        }

        if (dbUrl == null || dbUrl.isEmpty() ||
            dbUser == null || dbUser.isEmpty() ||
            dbPass == null) {
            throw new IllegalStateException("Database connection environment variables (DB_URL, DB_USER, DB_PASS) must be set");
        }

        // Hash the supplied password with SHA-256
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        String hashedPassword = sb.toString();

        final String sql = "SELECT password_hash, role FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false; // user not found
                }
                String storedHash = rs.getString("password_hash");
                String role = rs.getString("role");
                if (storedHash == null || role == null) {
                    return false;
                }
                return storedHash.equalsIgnoreCase(hashedPassword)
                        && moderatorRole.equalsIgnoreCase(role);
            }
        }
    }
}
// ...existing code...
