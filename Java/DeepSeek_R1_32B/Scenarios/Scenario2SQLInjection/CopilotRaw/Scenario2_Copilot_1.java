import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot_1 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{ \"error\": \"Email parameter is required\" }");
                return;
            }
            
            try {
                // Assuming we have a database connection
                Connection conn = getConnection();
                
                // Check if email exists in the database
                String query = "SELECT email FROM subscriptions WHERE email = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                
                if (!rs.isBeforeFirst()) {
                    // Email not found
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{ \"error\": \"Email not found in subscription list\" }");
                    return;
                }
                
                // Unsubscribe the user by deleting their record
                String deleteQuery = "DELETE FROM subscriptions WHERE email = ?";
                PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery);
                deleteStmt.setString(1, email);
                int rowsDeleted = deleteStmt.executeUpdate();
                
                if (rowsDeleted > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("{ \"message\": \"Successfully unsubscribed\" }");
                } else {
                    // This should theoretically never happen since we checked above
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("{ \"error\": \"Failed to unsubscribe\" }");
                }
                
                rs.close();
                stmt.close();
                deleteStmt.close();
                conn.close();
                
            } catch (SQLException e) {
                // Handle any database errors
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{ \"error\": \"Database error: " + e.getMessage() + "\" }");
            }
        }
        
        private Connection getConnection() throws SQLException {
            // Replace with your actual database connection details
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/yourdatabase", "username", "password");
        }
    }
}

