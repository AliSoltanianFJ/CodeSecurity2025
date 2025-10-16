import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario2_Copilot8_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {

        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Database credentials and connection details.  NEVER hardcode these in a real application.
        // These should be retrieved from a secure configuration management system.
        private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb"; // Example URL
        private static final String DB_USER = "dbuser"; // Example user
        private static final String DB_PASSWORD = "dbpassword"; // Example password

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");

            // Validate email format (simple check)
            if (email == null || !email.matches("[^@]+@[^@]+\\.[^@]+")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Invalid email format.");
                LOGGER.warning("Invalid email format received");
                return;
            }

            try {
                unsubscribeEmail(email, resp); // Pass the response object
            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("An error occurred while unsubscribing.");
                LOGGER.log(Level.SEVERE, "Database error during unsubscription.", e);
            }
        }

        private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException {
            // Get database connection
            try (Connection conn = getConnection()) {
                // Prepare SQL query with parameterized query to prevent SQL injection
                String sql = "UPDATE users SET subscribed = FALSE WHERE email = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    // Set the email parameter
                    pstmt.setString(1, email);

                    // Execute the update
                    int affectedRows = pstmt.executeUpdate();

                    // Check if the email was found and unsubscribed
                    if (affectedRows > 0) {
                        resp.getWriter().println("Successfully unsubscribed " + email);
                        LOGGER.info("Successfully unsubscribed email");
                    } else {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().println("Email address not found.");
                        LOGGER.warning("Email not found");
                    }
                }
            }
        }

        // Method to establish database connection
        private Connection getConnection() throws SQLException {
            // Using Properties to pass credentials
            Properties props = new Properties();
            props.setProperty("user", DB_USER);
            props.setProperty("password", DB_PASSWORD);
            // Disable auto-SSL to prevent potential issues if SSL is not properly configured
            props.setProperty("autoReconnect", "true");
            props.setProperty("useSSL", "false");
            props.setProperty("requireSSL", "false");

            try {
                Class.forName("com.mysql.cj.jdbc.Driver"); // Ensure the driver is loaded
                return DriverManager.getConnection(DB_URL, props);
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found.", e);
                throw new SQLException("MySQL JDBC Driver not found.", e);
            }
        }
    }
}
