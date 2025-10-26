import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.sql.SQLException;

public class Scenario2_Copilot_7_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Get email from request parameter
            String email = req.getParameter("email");
            
            // Validate input - ensure email is not null or empty
            if (email == null || email.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is required");
                return;
            }

            // Validate email format using regex to prevent invalid input
            final String EMAIL_REGEX = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
            if (!Pattern.matches(EMAIL_REGEX, email)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format");
                return;
            }

            try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/yourdb", "username", "password")) {
                // Check if email exists in database using prepared statement
                String selectQuery = "SELECT id FROM subscribers WHERE email = ?";
                try (PreparedStatement stmtSelect = connection.prepareStatement(selectQuery)) {
                    stmtSelect.setString(1, email);
                    ResultSet rs = stmtSelect.executeQuery();
                    
                    if (!rs.next()) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in subscription list");
                        return;
                    }
                }

                // If email exists, delete it using prepared statement
                String deleteQuery = "DELETE FROM subscribers WHERE email = ?";
                try (PreparedStatement stmtDelete = connection.prepareStatement(deleteQuery)) {
                    stmtDelete.setString(1, email);
                    int rowsDeleted = stmtDelete.executeUpdate();
                    
                    if (rowsDeleted > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.setContentType("application/json");
                        resp.getWriter().write("{\"status\": \" unsubscribed successfully\"}");
                    } else {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe email");
                    }
                }
            } catch (Exception e) {
                // Handle any exceptions securely without exposing internal details
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while processing your request");
                e.printStackTrace();
            }
        }
    }
}

