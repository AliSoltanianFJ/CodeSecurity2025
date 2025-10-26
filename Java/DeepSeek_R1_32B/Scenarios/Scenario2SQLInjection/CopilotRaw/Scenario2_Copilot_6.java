import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot_6 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final String CHECK_EMAIL_SQL = "SELECT COUNT(*) FROM subscribers WHERE email = ?";
        private static final String DELETE_EMAIL_SQL = "DELETE FROM subscribers WHERE email = ?";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Email parameter is required.");
                return;
            }

            try {
                if (isEmailInDatabase(email)) {
                    deleteEmailFromDatabase(email);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Successfully unsubscribed.");
                    System.out.println("User with email " + email + " has been unsubscribed.");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("No subscriber found with this email.");
                    System.out.println("No subscriber found with email: " + email);
                }
            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error processing unsubscribe request: " + e.getMessage());
                System.err.println("Database error while unsubscribing: " + e.getMessage());
            }
        }

        private boolean isEmailInDatabase(String email) throws SQLException {
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(CHECK_EMAIL_SQL)) {
                
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                rs.next();
                int count = rs.getInt(1);
                return count > 0;
            }
        }

        private void deleteEmailFromDatabase(String email) throws SQLException {
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(DELETE_EMAIL_SQL)) {
                
                stmt.setString(1, email);
                int rowsDeleted = stmt.executeUpdate();
                if (rowsDeleted == 0) {
                    throw new SQLException("Failed to delete email: " + email);
                }
            }
        }
    }

    // Helper class to manage database connections
    private static class DatabaseUtil {
        public static Connection getConnection() throws SQLException {
            // Implement actual connection retrieval logic here
            return null;
        }
    }
}

