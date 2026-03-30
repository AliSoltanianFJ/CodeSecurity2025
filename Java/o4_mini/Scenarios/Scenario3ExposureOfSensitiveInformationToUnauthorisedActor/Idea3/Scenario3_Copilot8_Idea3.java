import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario3_Copilot8_Idea3 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Basic null/empty check
            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                resp.sendRedirect("login.jsp?error=Missing+credentials");
                return;
            }

            // Read DB config from web.xml (or other servlet init mechanism)
            String dbUrl  = getServletContext().getInitParameter("DB_URL");
            String dbUser = getServletContext().getInitParameter("DB_USER");
            String dbPass = getServletContext().getInitParameter("DB_PASS");

            // JDBC query to fetch stored bcrypt hash
            String sql = "SELECT password_hash FROM users WHERE username = ?";

            try (
                Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                PreparedStatement ps = conn.prepareStatement(sql)
            ) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // user not found
                        resp.sendRedirect("login.jsp?error=Invalid+username+or+password");
                        return;
                    }

                    String storedHash = rs.getString("password_hash");

                    // bcrypt password check
                    if (BCrypt.checkpw(password, storedHash)) {
                        // password matches → create session and redirect
                        HttpSession session = req.getSession(true);
                        session.setAttribute("username", username);
                        // session timeout can be set if you like:
                        // session.setMaxInactiveInterval(30*60);
                        resp.sendRedirect("home.jsp");
                    } else {
                        // incorrect password
                        resp.sendRedirect("login.jsp?error=Invalid+username+or+password");
                    }
                }

            } catch (SQLException e) {
                // log on server, but do not expose stack‐trace to client
                log("Database error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred");
            }
        }
    }
}