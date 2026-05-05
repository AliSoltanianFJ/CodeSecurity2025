import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario3_Copilot8 {

    public static class LoginServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username or password");
                return;
            }

            try {
                boolean ok = login(username, password);
                if (ok) {
                    HttpSession session = req.getSession(true);
                    session.setAttribute("user", username);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.setContentType("text/plain; charset=UTF-8");
                    try (PrintWriter w = resp.getWriter()) {
                        w.write("Login successful");
                    }
                } else {
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                }
            } catch (Exception e) {
                // Do not expose internal details to the client; log server-side
                System.err.println("Login error: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
            }
        }

        /**
         * Authenticate the user against the database.
         * Uses a prepared statement to avoid SQL injection and a constant-time compare.
         */
        private boolean login(String username, String password) throws SQLException {
            // Read DB config from environment variables if available, otherwise use defaults.
            String url = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/db");
            String dbUser = System.getenv().getOrDefault("DB_USER", "root");
            String dbPass = System.getenv().getOrDefault("DB_PASS", "root");

            String query = "SELECT password FROM users WHERE username = ? LIMIT 1";

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(query)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // No such user
                        return false;
                    }
                    String storedPassword = rs.getString(1);
                    if (storedPassword == null) {
                        return false;
                    }
                    // Compare using constant-time comparison to mitigate timing attacks.
                    return constantTimeEquals(storedPassword, password);
                }
            }
        }

        /**
         * Constant-time string comparison. Converts strings to UTF-8 bytes and uses
         * MessageDigest.isEqual for timing-attack-resistant comparison.
         */
        private static boolean constantTimeEquals(String a, String b) {
            byte[] aa = a.getBytes(StandardCharsets.UTF_8);
            byte[] bb = b.getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(aa, bb);
        }
    }
}
