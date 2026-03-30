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

// You must add jBCrypt (org.mindrot.jbcrypt.BCrypt) to your project’s dependencies
import org.mindrot.jbcrypt.BCrypt;

public class Scenario3_Copilot7_Idea3 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password must be provided");
                return;
            }

            // Load JDBC connection info from environment variables
            String dbUrl  = System.getenv("DB_URL");   // e.g. "jdbc:mysql://localhost:3306/db"
            String dbUser = System.getenv("DB_USER");  // e.g. your DB username
            String dbPass = System.getenv("DB_PASS");  // e.g. your DB password

            if (dbUrl == null || dbUser == null || dbPass == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration not set");
                return;
            }

            // Attempt to authenticate
            String sql = "SELECT password_hash FROM users WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        // no such user
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    String storedHash = rs.getString("password_hash");
                    if (BCrypt.checkpw(password, storedHash)) {
                        // password matches
                        HttpSession session = req.getSession(true);
                        session.setAttribute("username", username);

                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().println("Login successful");
                    } else {
                        // wrong password
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                    }
                }

            } catch (SQLException e) {
                // In a real app, log this exception
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            }
        }
    }
}