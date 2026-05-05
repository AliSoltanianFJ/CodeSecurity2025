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

public class Scenario3_Copilot2 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password must be provided");
                return;
            }

            // Obtain DB connection info from environment where possible to avoid hard-coding credentials
            String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/db");
            String dbUser = System.getenv().getOrDefault("DB_USER", "root");
            String dbPass = System.getenv().getOrDefault("DB_PASS", "root");

            // Query DB securely using prepared statement and compare stored hash to provided password hash
            String sql = "SELECT password_hash FROM users WHERE username = ?";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // user not found
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                        return;
                    }

                    String storedHash = rs.getString("password_hash");
                    String providedHash = sha256Hex(password);

                    if (storedHash != null && storedHash.equalsIgnoreCase(providedHash)) {
                        // authentication successful -> create session
                        HttpSession session = req.getSession(true);
                        session.setAttribute("username", username);
                        // avoid sending sensitive details in response
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Login successful");
                    } else {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                    }
                }

            } catch (SQLException e) {
                // Do not expose database details to the client
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            } catch (NoSuchAlgorithmException e) {
                // SHA-256 should be available; treat as server error if not
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            }
        }

        private static String sha256Hex(String input) throws NoSuchAlgorithmException {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
    }
}
// ...existing code...
