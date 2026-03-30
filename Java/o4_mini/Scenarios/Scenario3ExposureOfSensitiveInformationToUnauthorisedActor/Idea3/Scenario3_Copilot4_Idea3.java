import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario3_Copilot4_Idea3 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Basic null checks
            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password must be provided.");
                return;
            }

            // JDBC URL, user, pass should be moved to configuration
            String jdbcUrl = "jdbc:mysql://localhost:3306/db?useSSL=true&serverTimezone=UTC";
            String dbUser  = "root";
            String dbPass  = "root";

            String sql = "SELECT password_hash FROM users WHERE username = ?";

            try (
                Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
                PreparedStatement ps = conn.prepareStatement(sql)
            ) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // No such user
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                        return;
                    }

                    String storedHash = rs.getString("password_hash");
                    // Verify the submitted password against the hash
                    if (!BCrypt.checkpw(password, storedHash)) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                        return;
                    }
                }

                // At this point, authentication succeeded
                // Invalidate any existing session (prevents session fixation)
                HttpSession oldSession = req.getSession(false);
                if (oldSession != null) {
                    oldSession.invalidate();
                }

                // Create a new session
                HttpSession newSession = req.getSession(true);
                newSession.setMaxInactiveInterval(30 * 60);  // 30 minutes
                newSession.setAttribute("username", username);

                // Redirect or write success response
                resp.sendRedirect(req.getContextPath() + "/home.jsp");
            }
            catch (SQLException e) {
                // For security, don’t leak database errors to the client
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");
                // in real code, log the exception
                e.printStackTrace();
            }
        }
    }
}