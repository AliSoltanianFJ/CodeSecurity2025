import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario3_Copilot3_idea1 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Validate input
            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password required.");
                return;
            }

            // Use environment variables or a secure config for credentials in production
            String dbUrl = "jdbc:mysql://localhost:3306/db";
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            if (dbUser == null || dbPass == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database credentials not set.");
                return;
            }

            try (Connection db = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                String sql = "SELECT password_hash FROM users WHERE username = ?";
                try (PreparedStatement stmt = db.prepareStatement(sql)) {
                    stmt.setString(1, username);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String storedHash = rs.getString("password_hash");
                            if (verifyPassword(password, storedHash)) {
                                // Login successful
                                resp.getWriter().write("Login successful!");
                            } else {
                                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                            }
                        } else {
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                        }
                    }
                }
            } catch (SQLException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
            }
        }

        // Example password verification using BCrypt (use a proper library in production)
        private boolean verifyPassword(String password, String storedHash) {
            // Replace with actual BCrypt verification, e.g.:
            // return BCrypt.checkpw(password, storedHash);
            // For demonstration, use a placeholder comparison:
            return password.equals(storedHash);
        }
    }
}
