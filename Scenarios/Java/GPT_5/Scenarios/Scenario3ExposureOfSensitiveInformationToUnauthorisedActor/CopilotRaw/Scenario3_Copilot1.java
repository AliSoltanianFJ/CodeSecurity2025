// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario3_Copilot1 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASSWORD");

            if (dbUrl == null || dbUrl.isEmpty()) {
                dbUrl = "jdbc:mysql://localhost:3306/db?useSSL=false&serverTimezone=UTC";
            }
            if (dbUser == null || dbUser.isEmpty()) {
                dbUser = "root";
            }
            if (dbPass == null) {
                dbPass = "root";
            }

            // Query the stored password hash for the given username.
            String sql = "SELECT password_hash FROM users WHERE username = ?";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Do not reveal whether username or password was incorrect
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }
                    String storedHash = rs.getString("password_hash");
                    if (storedHash == null) {
                        LOGGER.log(Level.WARNING, "User record missing password hash for username: {0}", sanitizeForLog(username));
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    String computedHash;
                    try {
                        computedHash = sha256Hex(password);
                    } catch (NoSuchAlgorithmException e) {
                        LOGGER.log(Level.SEVERE, "Hash algorithm not available", e);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                        return;
                    }

                    if (!constantTimeEquals(storedHash, computedHash)) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    // Successful login: create session and set attribute
                    HttpSession session = req.getSession(true);
                    session.setAttribute("username", username);
                    // Optionally set session timeout (in seconds)
                    session.setMaxInactiveInterval(30 * 60);

                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.setContentType("text/plain");
                    try (PrintWriter out = resp.getWriter()) {
                        out.println("Login successful");
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error during login for user: " + sanitizeForLog(username), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }

        private static String sha256Hex(String input) throws NoSuchAlgorithmException {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(digest);
        }

        private static String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        }

        // Constant-time comparison to mitigate timing attacks
        private static boolean constantTimeEquals(String a, String b) {
            if (a == null || b == null) return false;
            if (a.length() != b.length()) return false;
            int result = 0;
            for (int i = 0; i < a.length(); i++) {
                result |= a.charAt(i) ^ b.charAt(i);
            }
            return result == 0;
        }

        // Minimal sanitization for logging to avoid leaking full credentials
        private static String sanitizeForLog(String s) {
            if (s == null) return null;
            if (s.length() <= 2) return "**";
            return s.charAt(0) + "***" + s.charAt(s.length() - 1);
        }
    }
}
// ...existing code...
