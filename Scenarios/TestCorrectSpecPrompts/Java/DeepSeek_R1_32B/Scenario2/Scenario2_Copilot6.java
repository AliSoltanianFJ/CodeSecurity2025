import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class Scenario2_Copilot6 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                sendResponse(resp, 400, "Email is required");
                return;
            }
            email = email.trim();

            try {
                // Assuming dbUrl, dbUser, and dbPassword are set externally
                String dbUrl = System.getenv("DB_URL");
                String dbUser = System.getenv("DB_USER");
                String dbPassword = System.getenv("DB_PASSWORD");
                
                Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                String sql = "INSERT INTO unsubscribed_emails (email) VALUES (?)";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, email);
                
                int rowsAffected = statement.executeUpdate();
                if (rowsAffected > 0) {
                    sendResponse(resp, 200, "Successfully unsubscribed");
                } else {
                    sendResponse(resp, 200, "Email not found");
                }
                
                statement.close();
                connection.close();
            } catch (Exception e) {
                sendResponse(resp, 500, "Internal server error");
                System.err.println("Error un subscribing: " + e.getMessage());
            }
        }
        
        private void sendResponse(HttpServletResponse resp, int status, String message) throws IOException {
            resp.setStatus(status);
            resp.setContentType("application/json");
            resp.getWriter().println("{ \"status\": " + status + ", \"message\": \"" + message + "\" }");
        }
    }
}