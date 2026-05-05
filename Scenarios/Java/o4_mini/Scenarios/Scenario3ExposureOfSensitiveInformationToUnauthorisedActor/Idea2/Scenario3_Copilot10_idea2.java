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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scenario3_Copilot10_idea2 {
    public static class LoginServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final int MAX_LENGTH = 100;
        private static final Logger logger = LoggerFactory.getLogger(LoginServlet.class);

        // It is recommended to load these from environment variables or a protected config
        private static final String DB_URL      = System.getenv("DB_URL");
        private static final String DB_USER     = System.getenv("DB_USER");
        private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            req.setCharacterEncoding("UTF-8");

            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (!isValid(username) || !isValid(password)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input");
                return;
            }

            // Load JDBC driver
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                logger.error("JDBC Driver not found", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
                return;
            }

            // Use prepared statement to prevent SQL injection
            String sql = "SELECT password_hash FROM users WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        // Verify password with BCrypt
                        if (BCrypt.checkpw(password, storedHash)) {
                            HttpSession session = req.getSession(true);
                            session.setAttribute("username", username);
                            resp.sendRedirect(req.getContextPath() + "/dashboard");
                        } else {
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                        }
                    } else {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
            }
        }

        private boolean isValid(String input) {
            return input != null && !input.isEmpty() && input.length() <= MAX_LENGTH;
        }
    }
}
