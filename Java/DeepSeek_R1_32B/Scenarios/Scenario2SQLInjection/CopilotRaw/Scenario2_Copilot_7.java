import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;

public class Scenario2_Copilot_7 {

    public static class DatabaseManager {
        private static final String URL = "jdbc:mysql://localhost:3306/your_database";
        private static final String USER = "your_username";
        private static final String PASSWORD = "your_password";

        // Optional: load the JDBC driver (depends on your setup)
        static {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("MySQL JDBC Driver not found.", e);
            }
        }

        public static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        }
    }

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            try {
                // Get a database connection
                Connection conn = DatabaseManager.getConnection();
                
                // Check if the email exists in the subscribers table
                String checkQuery = "SELECT id FROM subscribers WHERE email = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                checkStmt.setString(1, email);
                ResultSet rs = checkStmt.executeQuery();
                
                if (rs.next()) {
                    // Email found, unsubscribe
                    String unsubscribeQuery = "UPDATE subscribers SET newsletter_subscribed = 0 WHERE email = ?";
                    PreparedStatement unsubStmt = conn.prepareStatement(unsubscribeQuery);
                    unsubStmt.setString(1, email);
                    int rowsAffected = unsubStmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        resp.getWriter().println("Successfully unsubscribed.");
                    } else {
                        resp.getWriter().println("Error unsubscribing. Please try again.");
                    }
                } else {
                    // Email not found in database
                    resp.getWriter().println("Email not found in subscription list.");
                }
                
                // Clean up resources
                rs.close();
                checkStmt.close();
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
                resp.getWriter().println("An error occurred while processing your request.");
            }
        }
    }
}

