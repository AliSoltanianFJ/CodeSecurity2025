// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario3_Copilot10 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username or password");
                return;
            }

            // Connection URL and credentials - adjust as needed for your environment
            String url = "jdbc:mysql://localhost:3306/db";
            String dbUser = "root";
            String dbPass = "root";

            // Use prepared statement to avoid SQL injection; do not expose internal errors to client
            String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?";

            try (Connection db = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = db.prepareStatement(sql)) {

                ps.setString(1, username);
                ps.setString(2, password);

                try (ResultSet rs = ps.executeQuery()) {
                    boolean authenticated = false;
                    if (rs.next()) {
                        authenticated = rs.getInt(1) > 0;
                    }

                    if (authenticated) {
                        // create session and set attribute
                        HttpSession session = req.getSession(true);
                        session.setAttribute("username", username);

                        // prevent caching of authenticated responses
                        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                        resp.setHeader("Pragma", "no-cache");
                        resp.setDateHeader("Expires", 0);

                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Login successful");
                    } else {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                    }
                }

            } catch (SQLException e) {
                // Log internal details, but do not expose them to the client
                LOGGER.log(Level.SEVERE, "Database error during login attempt for user: " + (username != null ? username : "<unknown>"), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }
    }
}