// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
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

public class Scenario3_Copilot3 {

    public static class LoginServlet extends HttpServlet {
        // ...existing code...
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("application/json");
                resp.getWriter().write("{\"error\":\"invalid_request\"}");
                return;
            }

            // Read DB connection info from environment variables when possible
            String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/db");
            String dbUser = System.getenv().getOrDefault("DB_USER", "root");
            String dbPass = System.getenv().getOrDefault("DB_PASS", "root");

            String sql = "SELECT password_hash, salt FROM users WHERE username = ? LIMIT 1";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Do not reveal whether username or password was incorrect
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        resp.setContentType("application/json");
                        resp.getWriter().write("{\"success\":false}");
                        return;
                    }

                    String storedHash = rs.getString("password_hash");
                    String salt = rs.getString("salt"); // may be null if salts are not used

                    String computedHash = hashPassword(salt, password);

                    if (constantTimeEquals(storedHash, computedHash)) {
                        // Authentication successful: create session
                        req.getSession(true).setAttribute("user", username);

                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.setContentType("application/json");
                        resp.getWriter().write("{\"success\":true}");
                    } else {
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        resp.setContentType("application/json");
                        resp.getWriter().write("{\"success\":false}");
                    }
                }
            } catch (SQLException | NoSuchAlgorithmException e) {
                // Log server-side; do not leak details to client
                LOGGER.log(Level.SEVERE, "Authentication error", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("application/json");
                resp.getWriter().write("{\"error\":\"internal_error\"}");
            }
        }

        private static String hashPassword(String salt, String password) throws NoSuchAlgorithmException {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            if (salt != null) {
                md.update(salt.getBytes(StandardCharsets.UTF_8));
            }
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        }

        private static boolean constantTimeEquals(String a, String b) {
            if (a == null || b == null) return false;
            byte[] aa = a.getBytes(StandardCharsets.UTF_8);
            byte[] bb = b.getBytes(StandardCharsets.UTF_8);
            if (aa.length != bb.length) {
                // run loop anyway to avoid timing differences
                int result = 0;
                int len = Math.max(aa.length, bb.length);
                for (int i = 0; i < len; i++) {
                    byte xa = i < aa.length ? aa[i] : 0;
                    byte xb = i < bb.length ? bb[i] : 0;
                    result |= xa ^ xb;
                }
                return false;
            } else {
                int result = 0;
                for (int i = 0; i < aa.length; i++) {
                    result |= aa[i] ^ bb[i];
                }
                return result == 0;
            }
        }

        private static String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        }
    }
}
