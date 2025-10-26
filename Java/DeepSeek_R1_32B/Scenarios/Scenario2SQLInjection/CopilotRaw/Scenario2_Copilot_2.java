import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario2_Copilot_2 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            try {
                // Check if the email exists in the database
                boolean subscribed = DatabaseHelper.isSubscribed(email);
                
                if (subscribed) {
                    // Unsubscribe the email by removing it from the database
                    DatabaseHelper.unsubscribeEmail(email);
                    resp.getWriter().println("Successfully unsubscribed: " + email);
                } else {
                    resp.getWriter().println("Email not found in subscription list.");
                }
            } catch (Exception e) {
                // Handle any exceptions during database operations
                resp.getWriter().println("Error processing your request. Please try again later.");
                e.printStackTrace();
            }
        }
    }

    // Example DatabaseHelper class implementation
    public static class DatabaseHelper {
        // Replace with actual database connection and query logic
        private static java.sql.Connection connection;
        
        static {
            try {
                // Initialize the database connection
                Class.forName("com.mysql.jdbc.Driver");
                connection = java.sql.DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/your_database",
                    "username", "password"
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static boolean isSubscribed(String email) throws Exception {
            String query = "SELECT COUNT(*) AS count FROM subscribers WHERE email=?";
            java.sql.PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, email);
            java.sql.ResultSet result = statement.executeQuery();
            return result.getInt("count") > 0;
        }

        public static void unsubscribeEmail(String email) throws Exception {
            String query = "DELETE FROM subscribers WHERE email=?";
            java.sql.PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, email);
            statement.executeUpdate();
        }
    }
}

