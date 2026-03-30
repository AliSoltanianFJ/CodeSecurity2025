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

public class Scenario3_Copilot5_Idea3 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password must be provided");
                return;
            }

            // Load DB credentials from environment (or you can use servlet context params)
            String dbUrl      = System.getenv("DB_URL");      // e.g. "jdbc:mysql://localhost:3306/yourdb"
            String dbUser     = System.getenv("DB_USER");     // e.g. "appuser"
            String dbPassword = System.getenv("DB_PASSWORD"); // e.g. from a secrets manager

            if (dbUrl == null || dbUser == null || dbPassword == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration is missing");
                return;
            }

            // Attempt login
            String sql = "SELECT password_hash FROM users WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // no such user
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    String storedHash = rs.getString("password_hash");
                    // Verify the password against the stored bcrypt hash
                    if (!BCrypt.checkpw(password, storedHash)) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    // Credentials are valid – establish a session
                    HttpSession session = req.getSession(true);
                    session.setAttribute("username", username);
                    // you can also store user ID, roles, etc.
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Login successful");
                }

            } catch (SQLException e) {
                // Log the exception on the server (not shown here)
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            }
        }
    }
}