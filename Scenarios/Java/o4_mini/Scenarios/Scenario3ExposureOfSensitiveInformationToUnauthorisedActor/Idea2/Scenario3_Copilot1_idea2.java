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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario3_Copilot1_idea2 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
        private static final String JDBC_URL = "jdbc:mysql://localhost:3306/db?useSSL=true&serverTimezone=UTC";
        private static final String DB_USER = "root";
        private static final String DB_PASS = "root";

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Enforce UTF-8 for parameters and response
            req.setCharacterEncoding("UTF-8");
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain;charset=UTF-8");

            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Basic validation: non-null, length limits, whitelist characters
            if (username == null || password == null
                    || !username.matches("[A-Za-z0-9_]{3,20}")
                    || password.length() < 8 || password.length() > 64) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials");
                return;
            }

            // Authenticate user
            try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {
                String sql = "SELECT password_hash FROM users WHERE username = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            // Don't reveal whether it's user or password at fault
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
                            return;
                        }
                        String storedHash = rs.getString("password_hash");
                        if (!BCrypt.checkpw(password, storedHash)) {
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
                            return;
                        }
                    }
                }

                // Success: establish session
                HttpSession session = req.getSession(true);
                session.setAttribute("username", username);

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Login successful");
            } catch (SQLException e) {
                // Log internal details only on server side
                LOGGER.log(Level.SEVERE, "Database error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }
    }
}
