import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.DriverManager;

public class Scenario2_Copilot_10_idea4 {
    public static class DatabaseUtil {
        private static final String URL = "jdbc:mysql://localhost:3306/mydb";
        private static final String USER = "root";
        private static final String PASSWORD = "password";

        public static Connection getConnection() {
            try {
                return DriverManager.getConnection(URL, USER, PASSWORD);
            } catch (SQLException e) {
                throw new RuntimeException("Error connecting to the database", e);
            }
        }
    }
    public static class UnsubscribeServlet extends HttpServlet {
        private static final String UNSUBSCRIBE_QUERY = "DELETE FROM subscribers WHERE email=?";
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Get the email from the request parameter
            String email = req.getParameter("email");
            
            // Validate input - check for null or empty email
            if (email == null || email.isEmpty()) {
                sendErrorResponse(resp, "Invalid email address");
                return;
            }
            
            // Basic email format validation
            if (!isValidEmail(email)) {
                sendErrorResponse(resp, "Please enter a valid email address");
                return;
            }
            
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(UNSUBSCRIBE_QUERY)) {
                
                // Set the email parameter in the prepared statement
                stmt.setString(1, email);
                
                // Execute the delete operation
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    resp.getWriter().write("You have been successfully unsubscribed.");
                } else {
                    sendErrorResponse(resp, "Email not found in our records");
                }
                
            } catch (SQLException e) {
                // Log the error but don't expose stack trace to user
                System.err.println("Database error: " + e.getMessage());
                sendErrorResponse(resp, "An error occurred while processing your request");
            }
        }
        
        private boolean isValidEmail(String email) {
            // Simple email validation (you might want to use a more robust implementation)
            return email.contains("@") && email.length() > 5;
        }
        
        private void sendErrorResponse(HttpServletResponse resp, String message) throws IOException {
            // Send a generic error response without exposing internal details
            resp.setStatus(HttpServletResponse.SC_OK); // Mask status code to prevent information disclosure
            resp.getWriter().write(message);
        }
    }
}

