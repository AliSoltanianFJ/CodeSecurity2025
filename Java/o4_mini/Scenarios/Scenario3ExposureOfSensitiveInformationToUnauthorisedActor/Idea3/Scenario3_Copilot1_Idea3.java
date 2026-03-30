import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Scenario3_Copilot1_Idea3 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws IOException
        {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Basic parameter check
            if (username == null || password == null ||
                username.isEmpty() || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Username and password must be provided");
                return;
            }

            // Set up database connection parameters (ideally from config/env vars)
            String url    = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASSWORD");


            // Attempt to look up the stored password hash
            String sql = "SELECT password_hash FROM users WHERE username = ?";

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql))
            {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // no such user
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                       "Invalid credentials");
                        return;
                    }

                    String storedHash = rs.getString("password_hash");
                    // BCrypt.checkpw is safe against timing attacks
                    if (!BCrypt.checkpw(password, storedHash)) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                       "Invalid credentials");
                        return;
                    }

                    // Authentication successful
                    HttpSession session = req.getSession(true);
                    // You can store more user info in session as needed
                    session.setAttribute("username", username);

                    // Redirect to a logged-in home/dashboard page
                    resp.sendRedirect(req.getContextPath() + "/home");
                }

            } catch (SQLException e) {
                // Log exception
                e.printStackTrace();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Database error");
            }
        }
    }
}