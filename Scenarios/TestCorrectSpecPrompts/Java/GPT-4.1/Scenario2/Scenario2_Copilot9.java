import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

public class Scenario2_Copilot9 {
    public static class UnsubscribeServlet extends HttpServlet {
        // It's recommended to use a connection pool in a real app
        private Connection getConnection() throws SQLException {
            // Use JNDI or environment configuration in real-world code
            // For this sample, simulated as an example:
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            resp.setContentType("text/plain");

            // Validate email parameter
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Email parameter is required.");
                return;
            }

            try (Connection conn = getConnection()) {
                // Parameterized query to prevent SQL injection
                String selectSql = "SELECT id FROM subscribers WHERE email = ?";
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, email);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        // Email found. Unsubscribe.
                        String deleteSql = "DELETE FROM subscribers WHERE email = ?";
                        try (PreparedStatement psDelete = conn.prepareStatement(deleteSql)) {
                            psDelete.setString(1, email);
                            psDelete.executeUpdate();
                        }
                        resp.getWriter().println("You have been unsubscribed.");
                    } else {
                        resp.getWriter().println("Email address not found.");
                    }
                }
            } catch (SQLException e) {
                // Do not expose stack trace to end user
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Internal server error. Please try again.");
                // Optionally, log the stack trace: e.printStackTrace();
            }
        }
    }
}