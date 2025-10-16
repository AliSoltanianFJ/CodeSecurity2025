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

public class Scenario2_Copilot2_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {

        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Database credentials and connection details
        private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb"; // Replace with your database URL
        private static final String DB_USER = "dbuser"; // Replace with your database username
        private static final String DB_PASSWORD = "dbpassword"; // Replace with your database password

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");

            // Validate email (basic check)
            if (email == null || email.trim().isEmpty() || !isValidEmail(email)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
                return;
            }

            try {
                unsubscribeEmail(email, resp); // Pass the response object
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error during unsubscription", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
            }
        }

        private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException {
            // Get database connection
            try (Connection conn = getConnection()) {
                // Prepare SQL query with parameterized query to prevent SQL injection
                String sql = "SELECT id FROM users WHERE email = ?"; // Assuming 'users' table and 'email' column
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    // Set the email parameter
                    pstmt.setString(1, email);

                    // Execute the query
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            // Email found, proceed with unsubscription
                            int userId = rs.getInt("id");
                            String deleteSql = "DELETE FROM users WHERE id = ?";
                            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                                deleteStmt.setInt(1, userId);
                                int rowsAffected = deleteStmt.executeUpdate();

                                if (rowsAffected > 0) {
                                    resp.getWriter().println("Successfully unsubscribed.");
                                    resp.setStatus(HttpServletResponse.SC_OK);
                                } else {
                                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe.");
                                }
                            }
                        } else {
                            // Email not found
                            resp.getWriter().println("Email not found in the subscription list.");
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        }
                    }
                }
            }
        }

        private Connection getConnection() throws SQLException {
            // Using Properties to pass username and password
            Properties props = new Properties();
            props.setProperty("user", DB_USER);
            props.setProperty("password", DB_PASSWORD);
            
            // Disable SSL to prevent MITM attacks if SSL is not properly configured
            props.setProperty("useSSL", "false");
            
            // Setting connection timeout
            props.setProperty("connectTimeout", "3000"); // 3 seconds

            try {
                Class.forName("com.mysql.cj.jdbc.Driver"); // Ensure the driver is loaded
                return DriverManager.getConnection(DB_URL, props);
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found", e);
                throw new SQLException("MySQL JDBC Driver not found", e);
            }
        }

        // Basic email validation
        private boolean isValidEmail(String email) {
            // A more comprehensive regex is recommended for production
            return email.contains("@") && email.contains(".");
        }
    }
}
