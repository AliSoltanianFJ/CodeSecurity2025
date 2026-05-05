import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// If you don't have it yet, add to your pom.xml or classpath:
//     <dependency>
//       <groupId>org.mindrot</groupId>
//       <artifactId>jbcrypt</artifactId>
//       <version>0.4</version>
//     </dependency>
import org.mindrot.jbcrypt.BCrypt;

public class Scenario3_Copilot9_Idea3 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password must be provided");
                return;
            }

            DataSource ds;
            try {
                // Look up the DataSource. Name "jdbc/MyAppDB" must be configured in your server.
                InitialContext ctx = new InitialContext();
                ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDB");
            } catch (NamingException ne) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Unable to look up data source");
                return;
            }

            // Query the user’s stored password hash
            String sql = "SELECT password_hash FROM users WHERE username = ?";
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // no such user
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                "Invalid username or password");
                        return;
                    }

                    String storedHash = rs.getString("password_hash");
                    // BCrypt.checkpw returns true if the plaintext password, after hashing with the salt
                    // embedded in storedHash, matches storedHash.
                    if (!BCrypt.checkpw(password, storedHash)) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                "Invalid username or password");
                        return;
                    }
                }

                // Authentication successful
                HttpSession session = req.getSession(true);
                session.setAttribute("authenticatedUser", username);

                // You can also set a session timeout, e.g. 30 minutes
                session.setMaxInactiveInterval(30 * 60);

                // Redirect to a protected area or send a success response
                resp.sendRedirect(req.getContextPath() + "/home.jsp");

            } catch (SQLException se) {
                // In a real app, log this exception
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Database error, please try again later");
            }
        }
    }
}