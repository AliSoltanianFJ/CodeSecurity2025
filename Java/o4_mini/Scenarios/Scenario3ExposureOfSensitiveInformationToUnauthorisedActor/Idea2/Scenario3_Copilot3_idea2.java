import javax.servlet.ServletException;
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

// You need the jBCrypt library on your classpath
import org.mindrot.jbcrypt.BCrypt;

public class Scenario3_Copilot3_idea2 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        // In real apps put these in environment variables or JNDI, not in code
        private static final String DB_URL = "jdbc:mysql://localhost:3306/db?useSSL=true&serverTimezone=UTC";
        private static final String DB_USER = "app_user";
        private static final String DB_PASS = "strongPassword";

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Enforce UTF-8 to avoid sneaky encoding attacks
            req.setCharacterEncoding("UTF-8");
            resp.setCharacterEncoding("UTF-8");

            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Basic input validation
            if (username == null || password == null
                    || username.length() < 3 || username.length() > 50
                    || password.length() < 8 || password.length() > 100
                    || !username.matches("[A-Za-z0-9_@.-]+")) {
                // Generic message so an attacker can’t enumerate valid users
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials");
                return;
            }

            // Lookup hashed password from DB
            String sql = "SELECT password_hash FROM users WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        // Compare using bcrypt
                        if (BCrypt.checkpw(password, storedHash)) {
                            // Authentication successful
                            HttpSession session = req.getSession();
                            session.setAttribute("authenticatedUser", username);
                            // Prevent session fixation
                            session.setMaxInactiveInterval(30 * 60);
                            resp.sendRedirect(req.getContextPath() + "/dashboard");
                            return;
                        }
                    }
                }
            } catch (SQLException e) {
                // Log internally, don’t expose stack traces
                LOGGER.log(Level.SEVERE, "Database error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                return;
            }

            // If we reach here, authentication failed
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
        }
    }
}
