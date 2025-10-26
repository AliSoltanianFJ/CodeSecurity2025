import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Scenario2_Copilot_9_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Retrieve the email from GET parameters
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email is required.");
                return;
            }

            try {
                // Validate email format
                if (!isValidEmail(email)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                    return;
                }
                
                // Sanitize input (escape special characters)
                String escapedEmail = escapeSqlInput(email);

                // Check if email exists in the database
                boolean isUnsubscribed = checkAndUnsubscribe(escapedEmail);
                
                if (isUnsubscribed) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Successfully unsubscribed.");
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in subscription list.");
                }
            } catch (SQLException e) {
                // Handle database errors
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "An error occurred while processing your request.");
                e.printStackTrace();
            }
        }

        private boolean isValidEmail(String email) {
            String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
            return email.matches(emailRegex);
        }

        private String escapeSqlInput(String sqlInput) throws SQLException {
            // Use PreparedStatement to prevent SQL injection
            // This method will be used in checkAndUnsubscribe
            return sqlInput.replace("\\", "\\\\");
        }

        private boolean checkAndUnsubscribe(String email) throws SQLException {
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id FROM users WHERE email = ?")) {

                pstmt.setString(1, email);
                ResultSet rs = pstmt.executeQuery();

                if (!rs.next()) {
                    return false;
                }

                // Delete the user from the database
                try (PreparedStatement deleteStmt = conn.prepareStatement(
                    "DELETE FROM users WHERE email = ?")) {

                    deleteStmt.setString(1, email);
                    int rowsAffected = deleteStmt.executeUpdate();
                    return rowsAffected > 0;
                }
            }
        }

        private Connection getConnection() throws SQLException {
            // Use a DataSource or JNDI lookup in production
            // For this example, we'll assume a secure connection is established
            // Replace with actual database connection logic
            throw new UnsupportedOperationException("Database connection not configured.");
        }
    }
}

