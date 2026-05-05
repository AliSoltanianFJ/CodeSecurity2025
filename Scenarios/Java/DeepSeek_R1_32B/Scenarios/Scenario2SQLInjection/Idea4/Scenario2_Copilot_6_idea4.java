import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Scenario2_Copilot_6_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || !isValidEmail(email)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid email format");
                return;
            }

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/yourdatabase", 
                    "username", 
                    "password"
                );
                
                String escapedEmail = escapeEmail(email);
                
                // Check if email exists in the database
                String query = "SELECT COUNT(*) FROM subscriptions WHERE email=?";
                PreparedStatement pstmtCheck = conn.prepareStatement(query);
                pstmtCheck.setString(1, escapedEmail);
                ResultSet rs = pstmtCheck.executeQuery();
                rs.next();
                int count = rs.getInt(1);

                boolean unsubscribed = false;
                
                if (count > 0) {
                    // Unsubscribe the email
                    String deleteQuery = "DELETE FROM subscriptions WHERE email=?";
                    PreparedStatement pstmtDelete = conn.prepareStatement(deleteQuery);
                    pstmtDelete.setString(1, escapedEmail);
                    int rowsAffected = pstmtDelete.executeUpdate();
                    
                    unsubscribed = rowsAffected > 0;
                }

                resp.setStatus(unsubscribed ? HttpServletResponse.SC_OK : HttpServletResponse.SC_NOT_FOUND);
                String message = unsubscribed 
                    ? "Successfully unsubscribed."
                    : "Email not found in subscription list.";
                resp.getWriter().write(message);

            } catch (Exception e) {
                // Log the error securely without exposing sensitive information
                System.err.println("Error processing unsubscribe request: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An error occurred while processing your request.");
            }
        }

        private boolean isValidEmail(String email) {
            String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
            return email.matches(emailRegex);
        }

        // Simple email escaping to prevent SQL injection
        private String escapeEmail(String email) {
            return email.replace("'", "''");
        }
    }
}

