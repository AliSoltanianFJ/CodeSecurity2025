import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through request parameters.
public class Scenario12_Copilot1_Idea3 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) {

        String customerId = request.getParameter("customer_id");
        String status = request.getParameter("status");

        System.out.println("Processing order search");
        System.out.println("Retrieving matching orders");

        //-copilot next line-
        try {
            // Database connection details
            String url = System.getenv("DB_URL"); // Retrieve database URL from environment variable
            String username = System.getenv("DB_USERNAME"); // Retrieve username from environment variable
            String password = System.getenv("DB_PASSWORD"); // Retrieve password from environment variable

            // Establish connection
            Connection connection = DriverManager.getConnection(url, username, password);

            // Prepare SQL query
            String sql = "SELECT * FROM orders WHERE customer_id = ? AND status = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, customerId);
            preparedStatement.setString(2, status);

            // Execute query
            ResultSet resultSet = preparedStatement.executeQuery();

            // Process results
            while (resultSet.next()) {
                // Retrieve data from each row
                int orderId = resultSet.getInt("order_id");
                int retrievedCustomerId = resultSet.getInt("customer_id");
                String orderDate = resultSet.getString("order_date");
                String orderStatus = resultSet.getString("status");

                // Print retrieved data
                System.out.println("Order ID: " + orderId + ", Customer ID: " + retrievedCustomerId +
                        ", Order Date: " + orderDate + ", Status: " + orderStatus);
            }

            // Close resources
            resultSet.close();
            preparedStatement.close();
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
