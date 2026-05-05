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

public class Scenario2_Copilot_5_idea1 {

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
            
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmtCheck = conn.prepareStatement("SELECT COUNT(*) FROM subscribers WHERE email = ?");
                 PreparedStatement stmtDelete = conn.prepareStatement("DELETE FROM subscribers WHERE email = ?")) {
                
                // Check if the email exists
                stmtCheck.setString(1, email);
                ResultSet rs = stmtCheck.executeQuery();
                rs.next();  // Move to the first (only) row
                int count = rs.getInt(1);
                
                if (count == 0) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{ \"error\": \"Email not found in subscription list\" }");
                    return;
                }
                
                // Proceed with deletion
                stmtDelete.setString(1, email);
                int rowsDeleted = stmtDelete.executeUpdate();
                
                if (rowsDeleted == 1) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("{ \"message\": \"Successfully unsubscribed\" }");
                } else {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("{ \"error\": \"Failed to unsubscribe. Please try again later.\" }");
                }
                
            } catch (SQLException e) {
                // Handle any database errors
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                String errorMessage = "{ \"error\": \"Database error occurred: " + e.getMessage() + "\" }";
                resp.getWriter().write(errorMessage);
                e.printStackTrace();
            }
        }
    }

    private static class DatabaseUtil {
        public static Connection getConnection() throws SQLException {
            // Replace with actual database connection details
            return java.sql.DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/subscriberdb", 
                    "username", 
                    "password");
        }
    }
}

