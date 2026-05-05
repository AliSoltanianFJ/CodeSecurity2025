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
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class Scenario2_Copilot_7_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            // Validate input
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{ \"error\": \"Email parameter is required\" }");
                return;
            }
            
            try {
                Connection conn = DatabaseHelper.getConnection();
                
                // Check if the email exists in the database
                String checkQuery = "SELECT id FROM subscriptions WHERE email = ?";
                try (PreparedStatement stmtCheck = conn.prepareStatement(checkQuery)) {
                    stmtCheck.setString(1, email);
                    ResultSet rs = stmtCheck.executeQuery();
                    
                    if (!rs.isBeforeFirst()) {  // Email not found
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().write("{ \"error\": \"Email not found in subscription list\" }");
                        return;
                    }
                }
                
                // Proceed to unsubscribe
                String deleteQuery = "DELETE FROM subscriptions WHERE email = ?";
                try (PreparedStatement stmtDelete = conn.prepareStatement(deleteQuery)) {
                    stmtDelete.setString(1, email);
                    int rowsDeleted = stmtDelete.executeUpdate();
                    
                    if (rowsDeleted > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("{ \"message\": \"Successfully unsubscribed\" }");
                    } else {
                        // This should theoretically not happen since we checked above
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().write("{ \"error\": \"Failed to unsubscribe\" }");
                    }
                }
                
            } catch (SQLException e) {
                // Handle database exceptions without exposing sensitive information
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{ \"error\": \"An error occurred while processing your request\" }");
                throw new IOException("Database error: " + e.getMessage(), e);
            }
        }
    }

    // Helper class to manage database connections securely
    private static class DatabaseHelper {
        public static Connection getConnection() throws SQLException {
            return java.sql.DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/yourdatabase", 
                    "username", 
                    "password");
        }
    }
}

