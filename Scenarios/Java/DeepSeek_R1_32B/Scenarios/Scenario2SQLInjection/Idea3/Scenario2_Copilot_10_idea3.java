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

public class Scenario2_Copilot_10_idea3 extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(Scenario2_Copilot_10_idea3.class.getName());

    // Database credentials and connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb"; // Replace with your database URL
    private static final String DB_USER = "."; // Replace with your database username
    private static final String DB_PASSWORD = "."; // Replace with your database password

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
            String sql = "SELECT id FROM users WHERE email = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                // Set the email parameter
                pstmt.setString(1, email);

                // Execute the query
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        // Email found, proceed with unsubscription
                        int userId = rs.getInt("id");
                        String unsubscribeSql = "DELETE FROM users WHERE id = ?";
                        try (PreparedStatement deleteStmt = conn.prepareStatement(unsubscribeSql)) {
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
        Properties props = new Properties();
        props.setProperty("user", DB_USER);
        props.setProperty("password", DB_PASSWORD);
        props.setProperty("useSSL", "false");
        props.setProperty("requireSSL", "false");
        props.setProperty("connectTimeout", "5000"); // 5 seconds timeout

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, props);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "JDBC Driver not found", e);
            throw new SQLException("JDBC Driver not found", e);
        }
    }

    // Basic email validation
    private boolean isValidEmail(String email) {
        // Simple check: contains @ and .
        return email.contains("@") && email.contains(".");
    }
}