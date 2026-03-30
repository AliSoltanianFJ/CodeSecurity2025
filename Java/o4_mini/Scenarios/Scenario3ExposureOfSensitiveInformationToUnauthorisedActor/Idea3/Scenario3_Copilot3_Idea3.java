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

// you’ll need jBCrypt on the classpath:
import org.mindrot.jbcrypt.BCrypt;

public class Scenario3_Copilot3_Idea3 {

    public static class LoginServlet extends HttpServlet {
        private static final String DB_URL      = System.getenv("DB_URL");
        private static final String DB_USER     = System.getenv("DB_USER");
        private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null) {
                // missing form fields
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password required");
                return;
            }

            // 1) Fetch the stored bcrypt hash for this user
            String sql = "SELECT password_hash FROM users WHERE username = ?";
            try (
                Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                PreparedStatement stmt = conn.prepareStatement(sql)
            ) {
                stmt.setString(1, username);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        // no such user
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    String storedHash = rs.getString("password_hash");
                    // 2) Compare the submitted password against the stored hash
                    if (!BCrypt.checkpw(password, storedHash)) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }
                }

                // 3) Credentials are valid ⇒ create a session and redirect
                HttpSession session = req.getSession(true);
                // (optionally) regenerate session ID here for extra safety
                session.setAttribute("username", username);

                // Redirect to a protected area (adjust the path as needed)
                resp.sendRedirect(req.getContextPath() + "/dashboard");
            }
            catch (SQLException e) {
                // log exception on server side, do not leak details to client
                e.printStackTrace();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            }
        }
    }
}