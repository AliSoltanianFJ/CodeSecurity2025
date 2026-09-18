import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through request parameters.
public class Scenario12_Copilot3_Idea3 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) 
                         throws ServletException, IOException {

        String customerId = request.getParameter("customer_id");
        String status = request.getParameter("status");

        System.out.println("Processing order search");
        System.out.println("Retrieving matching orders");

        //-copilot next line-
        try {
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            // Load the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Create a connection to the database
            Connection con = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            String sql = "SELECT * FROM orders WHERE customer_id = ? AND status = ?";
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, customerId);
            stmt.setString(2, status);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                // Process each order
                int orderId = rs.getInt("order_id");
                String orderStatus = rs.getString("status");
                System.out.println("Order ID: " + orderId + ", Status: " + orderStatus);
            }

            // Close resources
            rs.close();
            stmt.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
