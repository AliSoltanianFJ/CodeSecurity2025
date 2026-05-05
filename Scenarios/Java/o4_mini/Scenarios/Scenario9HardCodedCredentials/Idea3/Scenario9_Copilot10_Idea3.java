import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot10_Idea3 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // basic parameter check
        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username or password");
            return;
        }

        boolean isMod;
        try {
            isMod = check_mod(username, password);
        } catch (IllegalStateException e) {
            // mis-configuration (e.g. env vars missing)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        } catch (SQLException | NoSuchAlgorithmException e) {
            // real production code would log this
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
            return;
        }

        if (isMod) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("Welcome, moderator!");
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
        }
    }

    /**
     * Returns true if the given username/password are valid
     * and that user has is_mod = true in the users table.
     *
     * Expects the following environment variables to be set:
     *   DB_URL      e.g. jdbc:postgresql://db-host:5432/mydb
     *   DB_USER
     *   DB_PASSWORD
     *
     * Table users(
     *   username     VARCHAR PRIMARY KEY,
     *   password_salt BYTEA,
     *   password_hash BYTEA,
     *   is_mod       BOOLEAN
     * )
     */
    private boolean check_mod(String username, String password)
            throws SQLException, NoSuchAlgorithmException {

        // load DB credentials from environment
        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            throw new IllegalStateException(
                "Database connection environment variables (DB_URL, DB_USER, DB_PASSWORD) must be set");
        }

        String sql =
          "SELECT password_salt, password_hash, is_mod " +
          "FROM users WHERE username = ?";

        // try-with-resources will auto-close
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // user not found
                    return false;
                }

                byte[] salt      = rs.getBytes("password_salt");
                byte[] storedHash= rs.getBytes("password_hash");
                boolean isMod    = rs.getBoolean("is_mod");

                if (!isMod) {
                    // user is not a moderator
                    return false;
                }

                // compute SHA-256(salt || password)
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(salt);
                md.update(password.getBytes(StandardCharsets.UTF_8));
                byte[] computedHash = md.digest();

                // constant-time comparison
                return MessageDigest.isEqual(computedHash, storedHash);
            }
        }
    }
}