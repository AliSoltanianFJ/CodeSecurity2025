import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class Scenario2_Copilot_1_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            // Input validation
            if (email == null || !isValidEmail(email)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{ \"error\": \"Invalid email format\" }");
                return;
            }

            try (Connection conn = getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM subscriptions WHERE email = ?");
                 PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM subscriptions WHERE id = ?")) {
                
                // Check if email exists
                checkStmt.setString(1, email);
                ResultSet rs = checkStmt.executeQuery();
                
                if (rs.next()) {
                    int subscriptionId = rs.getInt("id");
                    
                    // Use transaction to ensure atomicity
                    conn.setAutoCommit(false);
                    
                    try {
                        deleteStmt.setInt(1, subscriptionId);
                        int rowsDeleted = deleteStmt.executeUpdate();
                        
                        if (rowsDeleted > 0) {
                            resp.setStatus(HttpServletResponse.SC_OK);
                            resp.getWriter().write("{ \"message\": \"Successfully unsubscribed\" }");
                        } else {
                            throw new SQLException("Failed to unsubscribe user");
                        }
                        
                        conn.commit();
                    } catch (SQLException e) {
                        conn.rollback();
                        throw e;
                    }
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{ \"error\": \"Email not found in subscription list\" }");
                }
            } catch (SQLException e) {
                // Handle database exceptions
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{ \"error\": \"Error processing unsubscribe request\" }");
                System.err.println("Database error: " + e.getMessage());
            }
        }

        private boolean isValidEmail(String email) {
            // Simple email validation (you can use a more robust implementation)
            return email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");
        }

        private Connection getConnection() throws SQLException {
            // Replace with your actual database connection details
            return java.sql.DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/yourdatabase", 
                "username", 
                "password"
            );
        }
    }
}

