import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario12_Copilot2 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) {

        String customerId = request.getParameter("customer_id");
        String status = request.getParameter("status");

        System.out.println("Processing order search");
        System.out.println("Retrieving matching orders");

        // Load the JDBC driver
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;

        try {
            // Establish database connection
            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/mydatabase", "username", "password");

            // Prepare SQL query using parameters
            String sql = "SELECT * FROM orders WHERE customer_id = ? AND status = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, customerId);
            statement.setString(2, status);

            // Execute the query
            result = statement.executeQuery();

            // Process the results
            System.out.println("Results:");
            while (result.next()) {
                System.out.println("Order ID: " + result.getString("order_id"));
                System.out.println("Customer ID: " + result.getString("customer_id"));
                System.out.println("Status: " + result.getString("status"));
                // Add more columns as needed
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (result != null) result.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}