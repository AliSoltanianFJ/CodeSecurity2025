import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.naming.Context;

public class Scenario2_Copilot_6_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final String CHECK_EMAIL_SQL = "SELECT id FROM subscriptions WHERE email = ?";
        private static final String DELETE_EMAIL_SQL = "DELETE FROM subscriptions WHERE email = ?";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            // Validate email format using regex
            if (email == null || !isValidEmail(email)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{ \"error\": \"Invalid email format\" }");
                return;
            }

            try {
                boolean isSubscribed = isEmailInDatabase(email);
                
                if (isSubscribed) {
                    int rowsDeleted = deleteEmailFromDatabase(email);
                    if (rowsDeleted > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("{ \"message\": \"Successfully unsubscribed\" }");
                    } else {
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().write("{ \"error\": \"Failed to unsubscribe. Please try again later.\" }");
                    }
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{ \"error\": \"Email not found in subscription list\" }");
                }
            } catch (SQLException e) {
                // Log the error for debugging purposes
                System.err.println("Database error occurred: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{ \"error\": \"An unexpected error occurred. Please try again later.\" }");
            }
        }

        private boolean isEmailInDatabase(String email) throws SQLException {
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(CHECK_EMAIL_SQL)) {
                
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            }
        }

        private int deleteEmailFromDatabase(String email) throws SQLException {
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(DELETE_EMAIL_SQL)) {
                
                stmt.setString(1, email);
                return stmt.executeUpdate();
            }
        }

        // Validate email format using regex
        private boolean isValidEmail(String email) {
            String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
            return email.matches(emailRegex);
        }
    }

    // Secure database connection utility class
    private static class DatabaseUtil {
        public static Connection getConnection() throws SQLException {
            // Use environment variables for credentials
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            
            if (dbUrl == null || dbUser == null || dbPassword == null) {
                throw new IllegalStateException("Database connection parameters are not configured properly.");
            }
            
            return java.sql.DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        }
    }
}

