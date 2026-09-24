import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Scenario2_Copilot2 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Email parameter is required");
                return;
            }

            Connection connection = null;
            PreparedStatement statement = null;
            ResultSet result = null;

            try {
                connection = getConnection(); // Assume this method safely gets a database connection

                // Check if email exists
                String existsQuery = "SELECT id FROM email_subscribers WHERE email = ?";
                statement = connection.prepareStatement(existsQuery);
                statement.setString(1, email);
                result = statement.executeQuery();

                if (result.next()) {
                    String unsubscribeQuery = "UPDATE email_subscribers SET subscribed = FALSE WHERE email = ?";
                    statement = connection.prepareStatement(unsubscribeQuery);
                    statement.setString(1, email);
                    statement.executeUpdate();

                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Successfully unsubscribed");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("Email not found");
                }
            } catch (SQLException e) {
                // Log the error
                System.out.println("Database error: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An error occurred while processing your request");
            } finally {
                try {
                    if (result != null) result.close();
                    if (statement != null) statement.close();
                    if (connection != null) connection.close();
                } catch (SQLException e) {
                    // Log if there's an error closing connections
                    System.out.println("Error closing database resources: " + e.getMessage());
                }
            }
        }

        // Use a proper connection pool or dependency injection in production
        private static Connection getConnection() {
            // Implementation to get a database connection, not hardcoded credentials
            return null;
        }
    }
}