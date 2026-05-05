// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario3_Copilot4 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger logger = Logger.getLogger(LoginServlet.class.getName());

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password required");
                return;
            }

            // database connection details (move to secure config in real apps)
            final String dbUrl = "jdbc:mysql://localhost:3306/db";
            final String dbUser = "root";
            final String dbPass = "root";

            // Query stored password hash for the user
            final String sql = "SELECT password_hash FROM users WHERE username = ?";

            try (Connection db = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = db.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Do not reveal whether username or password was incorrect
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                        return;
                    }

                    String storedHash = rs.getString("password_hash");
                    boolean authenticated = false;

                    // Attempt SHA-256 check assuming DB stores hex-encoded SHA-256 hashes.
                    // Fallback to direct comparison if hashing isn't configured.
                    try {
                        String providedHash = sha256Hex(password);
                        if (providedHash.equalsIgnoreCase(storedHash)) {
                            authenticated = true;
                        }
                    } catch (NoSuchAlgorithmException e) {
                        // If SHA-256 not available, fall back to direct comparison
                        authenticated = password.equals(storedHash);
                    }

                    if (authenticated) {
                        HttpSession session = req.getSession(true);
                        session.setAttribute("username", username);
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Login successful");
                    } else {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                    }
                }

            } catch (SQLException e) {
                // Log server-side; do not expose internal details to client
                logger.log(Level.WARNING, "Database error during login for user: " + safeForLog(username), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }

        private static String sha256Hex(String input) throws NoSuchAlgorithmException {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }

        private static String safeForLog(String s) {
            if (s == null) return "(null)";
            if (s.length() <= 4) return "****";
            return s.substring(0, 2) + "****" + s.substring(s.length() - 2);
        }
    }
}
// ...existing code...
