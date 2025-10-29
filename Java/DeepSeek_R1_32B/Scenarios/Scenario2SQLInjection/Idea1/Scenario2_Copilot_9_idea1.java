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

public class Scenario2_Copilot_9_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final String CHECK_EMAIL_SQL = "SELECT id FROM subscriptions WHERE email = ?";
        private static final String DELETE_EMAIL_SQL = "DELETE FROM subscriptions WHERE id = ?";
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{ \"error\": \"Email parameter is required\" }");
                return;
            }
            
            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement(CHECK_EMAIL_SQL)) {
                
                checkStmt.setString(1, email);
                ResultSet rs = checkStmt.executeQuery();
                
                if (!rs.isBeforeFirst()) { // Email not found
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{ \"error\": \"Email not found in subscription list\" }");
                    return;
                }
                
                // Proceed to unsubscribe
                int userId = rs.getInt("id");
                try (PreparedStatement deleteStmt = conn.prepareStatement(DELETE_EMAIL_SQL)) {
                    deleteStmt.setInt(1, userId);
                    int rowsDeleted = deleteStmt.executeUpdate();
                    
                    if (rowsDeleted > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("{ \"message\": \"Successfully unsubscribed\" }");
                    } else {
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().write("{ \"error\": \"Failed to unsubscribe\" }");
                    }
                }
            } catch (SQLException e) {
                // Log the error and inform the client
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{ \"error\": \"Error processing your request. Please try again later.\" }");
                e.printStackTrace();
            }
        }
    }

    private static class DBUtils {
        public static Connection getConnection() throws SQLException {
            // Replace with actual database connection details
            return java.sql.DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/yourdatabase", 
                    "username", 
                    "password");
        }
    }
}

