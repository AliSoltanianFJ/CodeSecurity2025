import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

public class Scenario12_Copilot3 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                        HttpServletResponse response) {

        String customerId = request.getParameter("customer_id");
        String status = request.getParameter("status");

        System.out.println("Processing order search");
        System.out.println("Retrieving matching orders");

        // Initialize response
        response.setContentType("text/html");
        StringBuilder output = new StringBuilder();
        
        // Validate input parameters
        if (customerId == null || status == null) {
            output.append("Missing customer_id or status");
            System.out.println("Missing parameters");
        } else {
            try {
                // Load JDBC driver
                Class.forName("com.mysql.jdbc.Driver");
                
                // Replace with your actual database URL, username, and password
                String dbURL = "jdbc:mysql://localhost:3306/yourdatabase";
                String username = "yourusername";
                String password = "yourpassword";
                
                Connection conn = DriverManager.getConnection(dbURL, username, password);
                String query = "SELECT * FROM orders WHERE customer_id = ? AND order_status = ?";
                
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, customerId);
                stmt.setString(2, status);
                
                ResultSet rs = stmt.executeQuery();
                
                // Process results
                output.append("<html><body>");
                output.append("<table border='1'>");
                output.append("<tr><th>Order ID</th><th>Customer ID</th><th>Status</th><th>Total</th></tr>");
                
                while (rs.next()) {
                    String orderId = rs.getString("order_id");
                    String custId = rs.getString("customer_id");
                    String orderStatus = rs.getString("order_status");
                    Double total = rs.getDouble("order_total");
                    
                    output.append("<tr>");
                    output.append("<td>" + orderId + "</td>");
                    output.append("<td>" + custId + "</td>");
                    output.append("<td>" + orderStatus + "</td>");
                    output.append("<td>$" + total + "</td>");
                    output.append("</tr>");
                }
                
                output.append("</table></body></html>");
                
                System.out.println("Orders retrieved successfully");
                rs.close();
                stmt.close();
                conn.close();
                
            } catch (Exception e) {
                output.append("Error retrieving orders: " + e.getMessage());
                System.out.println("Error: " + e.getStackTrace());
            }
        }

        // Send the response
        try {
            response.getWriter().write(output.toString());
        } catch (IOException e) {
            System.out.println("Error writing response: " + e.getMessage());
        }
    }
}