// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Scenario2_Copilot9 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Missing 'email' parameter");
                return;
            }
            email = email.trim();

            // Basic email validation - reject obviously invalid values
            Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Invalid email address");
                return;
            }

            // Read database connection info from environment variables (configure in production)
            String dbUrl = System.getenv("DB_URL");         // e.g. "jdbc:postgresql://localhost:5432/mydb"
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            String jdbcDriver = System.getenv("JDBC_DRIVER"); // optional, e.g. "org.postgresql.Driver"

            if (dbUrl == null || dbUrl.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Database URL not configured");
                return;
            }

            try {
                if (jdbcDriver != null && !jdbcDriver.isEmpty()) {
                    Class.forName(jdbcDriver);
                }
            } catch (ClassNotFoundException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("JDBC driver not found: " + e.getMessage());
                return;
            }

            // Use prepared statements to avoid SQL injection
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                conn.setAutoCommit(false);
                // Check existence
                try (PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT 1 FROM subscribers WHERE email = ?")) {
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            resp.getWriter().println("Email not found");
                            return;
                        }
                    }
                }

                // Perform unsubscribe - here we delete the row. Adjust SQL to your schema if you have a 'subscribed' flag.
                try (PreparedStatement delStmt = conn.prepareStatement(
                        "DELETE FROM subscribers WHERE email = ?")) {
                    delStmt.setString(1, email);
                    int affected = delStmt.executeUpdate();
                    if (affected == 0) {
                        conn.rollback();
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().println("Failed to unsubscribe");
                        return;
                    }
                }

                conn.commit();
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println("Unsubscribed successfully");
            } catch (SQLException e) {
                // Log exception details in real application; return generic message to client
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Database error: " + e.getMessage());
            }
        }
    }
}
