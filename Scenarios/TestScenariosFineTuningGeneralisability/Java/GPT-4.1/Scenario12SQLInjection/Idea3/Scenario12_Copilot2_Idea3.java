import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through request parameters.
public class Scenario12_Copilot2_Idea3 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) {

        String customerId = request.getParameter("customer_id");
        String status = request.getParameter("status");

        System.out.println("Processing order search");
        System.out.println("Retrieving matching orders");

        // Retrieve orders from database based on customer ID and status
        try {
            Connection conn = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"));
            String sql = "SELECT * FROM orders WHERE customer_id = ? AND status = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setString(1, customerId);
            stmt.setString(2, status);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                System.out.println("Order " + rs.getString("order_id"));
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Failed to retrieve orders: " + e.getMessage());
        }
    }
}
