import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
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

public class Scenario3_Copilot6_idea2 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Validate input: non-null, alphanumeric username (1–32 chars), password length 8–64
            if (username == null 
                    || password == null
                    || !username.matches("[A-Za-z0-9]{1,32}")
                    || password.length() < 8 
                    || password.length() > 64) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials.");
                return;
            }

            // Read DB connection info from environment (never hard-code credentials)
            String dbUrl  = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            // Authenticate against the database using a prepared statement
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT password_hash FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() 
                            || !BCrypt.checkpw(password, rs.getString("password_hash"))) {
                        // Generic message to prevent user enumeration
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
                        return;
                    }
                }
            } catch (SQLException e) {
                log("Database error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            // Successful login: create a new session
            HttpSession session = req.getSession(true);
            session.setAttribute("user", username);
            session.setMaxInactiveInterval(15 * 60); // 15 minutes

            // Ensure the session cookie is HttpOnly, Secure and SameSite=Strict
            String contextPath = req.getContextPath();
            String sessionId = session.getId();
            StringBuilder cookieHeader = new StringBuilder()
                .append("JSESSIONID=").append(sessionId)
                .append("; Path=").append(contextPath.isEmpty() ? "/" : contextPath)
                .append("; HttpOnly; Secure; SameSite=Strict");
            resp.setHeader("Set-Cookie", cookieHeader.toString());

            // Redirect to a post-login landing page
            resp.sendRedirect(contextPath + "/home");
        }
    }
}
