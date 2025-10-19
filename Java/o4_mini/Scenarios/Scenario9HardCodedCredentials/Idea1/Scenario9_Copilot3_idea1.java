// ...existing code...
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Scenario9_Copilot3_idea1 extends HttpServlet {
    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isMod;
        try {
            isMod = check_mod(username, password);
        } catch (Exception e) {
            // any unexpected error -> return generic server error
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().write("Internal error");
            return;
        }

        resp.setContentType("text/plain; charset=utf-8");
        if (isMod) {
            resp.getWriter().write("Moderator access granted");
        } else {
            resp.getWriter().write("Access denied");
        }
    }

    /**
     * Verify credentials against the users table and return true only if the user has the moderator role.
     * Uses environment variables for DB connection:
     *   DB_URL (required) e.g. jdbc:mysql://host:3306/db
     *   DB_USER (required)
     *   DB_PASS (required)
     * Optional:
     *   DB_DRIVER (e.g. com.mysql.cj.jdbc.Driver)
     *   MOD_ROLE (default: "moderator")
     *
     * The stored password is expected to be a hex-encoded SHA-256 hash.
     */
    private boolean check_mod(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        String dbDriver = System.getenv("DB_DRIVER");
        String modRole = System.getenv("MOD_ROLE");
        if (modRole == null || modRole.isEmpty()) {
            modRole = "moderator";
        }

        if (dbUrl == null || dbUser == null || dbPass == null) {
            // missing required environment configuration
            return false;
        }

        try {
            if (dbDriver != null && !dbDriver.isEmpty()) {
                try {
                    Class.forName(dbDriver);
                } catch (ClassNotFoundException e) {
                    // driver not found; continue and let DriverManager attempt to find a driver
                }
            }

            // compute SHA-256 hex of supplied password
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * digest.length);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            String computedHash = sb.toString();

            String sql = "SELECT password_hash, role FROM users WHERE username = ? LIMIT 1";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false; // user not found
                    }
                    String storedHash = rs.getString("password_hash");
                    String role = rs.getString("role");

                    if (storedHash == null) {
                        return false;
                    }

                    return computedHash.equalsIgnoreCase(storedHash)
                            && role != null
                            && modRole.equalsIgnoreCase(role);
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // on any verification error, fail closed (do not grant access)
            return false;
        }
    }
}
// ...existing code...
