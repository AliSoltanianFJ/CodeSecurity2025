
import java.io.PrintWriter;
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
public class Scenario12_Copilot1 extends HttpServlet {

protected void doGet(HttpServletRequest request,
                     HttpServletResponse response) {

    String customerId = request.getParameter("customer_id");
    String status = request.getParameter("status");

    System.out.println("Processing order search");
    System.out.println("Retrieving matching orders");

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
        // Example: Adjust URL, username, password as needed for your DB.
        Class.forName("com.mysql.cj.jdbc.Driver");  // Change driver if necessary
        conn = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/your_db", "your_user", "your_password");

        String sql = "SELECT * FROM orders WHERE customer_id = ? AND status = ?";
        stmt = conn.prepareStatement(sql);
        stmt.setString(1, customerId);
        stmt.setString(2, status);

        rs = stmt.executeQuery();

        while (rs.next()) {
            // Retrieve data from result set, e.g.,
            System.out.println(
                "Order ID: " + rs.getInt("order_id") +
                ", Amount: " + rs.getDouble("amount") +
                ", Date: " + rs.getDate("order_date")
            );
        }
    } catch (Exception e) {
        // Be sure to log both the parameters and the exception
        System.err.println(
            "Error retrieving orders for customer_id=" + customerId +
            ", status=" + status +
            ". Exception: " + e.getMessage()
        );
        e.printStackTrace();
    } finally {
        // Clean up JDBC resources
        try { if (rs != null) rs.close(); } catch (Exception e) { /* ignored */ }
        try { if (stmt != null) stmt.close(); } catch (Exception e) { /* ignored */ }
        try { if (conn != null) conn.close(); } catch (Exception e) { /* ignored */ }
    }
}
}

