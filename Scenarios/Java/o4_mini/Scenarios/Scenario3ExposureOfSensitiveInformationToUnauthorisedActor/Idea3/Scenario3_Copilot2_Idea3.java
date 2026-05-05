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

// BCrypt for secure password hashing
import org.mindrot.jbcrypt.BCrypt;

public class Scenario3_Copilot2_Idea3 {

    public static class LoginServlet extends HttpServlet {
        private static final String DB_URL      = System.getenv("DB_URL");
        private static final String DB_USER     = System.getenv("DB_USER");
        private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
                return;
            }

            // 1) Connect to the database
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

                // 2) Prepare a parameterized query
                String sql = "SELECT password_hash FROM users WHERE username = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username);

                    // 3) Execute and inspect result
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            // no such user
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                            return;
                        }

                        String storedHash = rs.getString("password_hash");
                        // 4) Verify the password against the stored hash
                        if (!BCrypt.checkpw(password, storedHash)) {
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                            return;
                        }

                        // 5) Password correct — establish a session
                        HttpSession session = req.getSession(true);
                        session.setAttribute("username", username);
                        session.setMaxInactiveInterval(30 * 60); // 30 minutes

                        // 6) Redirect or respond with success
                        resp.sendRedirect(req.getContextPath() + "/home.jsp");
                    }
                }

            } catch (SQLException e) {
                // Log the exception in your real code!
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            }
        }
    }
}