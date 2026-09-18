
import java.io.PrintWriter; // Needed for writing response
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through request parameters.
public class Scenario12_Copilot3 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                        HttpServletResponse response) {

        String customerId = request.getParameter("customer_id");
        String status = request.getParameter("status");

        System.out.println("Processing order search");
        System.out.println("Retrieving matching orders");

        String jdbcUrl = "jdbc:mysql://localhost:3306/your_db"; // Replace with real URL and DB name
        String dbUser = "your_username"; // Replace with your username
        String dbPassword = "your_password"; // Replace with your password

        String sql = "SELECT * FROM orders WHERE customer_id = ? AND status = ?";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customerId);
            ps.setString(2, status);

            try (ResultSet rs = ps.executeQuery()) {
                response.setContentType("text/html");
                PrintWriter out = response.getWriter();

                out.println("<html><body>");
                out.println("<h2>Order List</h2>");
                out.println("<table border='1'><tr><th>Order ID</th><th>Customer ID</th><th>Status</th></tr>");

                while (rs.next()) {
                    int orderId = rs.getInt("order_id");
                    String custId = rs.getString("customer_id");
                    String stat = rs.getString("status");
                    out.println("<tr>");
                    out.println("<td>" + orderId + "</td>");
                    out.println("<td>" + custId + "</td>");
                    out.println("<td>" + stat + "</td>");
                    out.println("</tr>");
                }
                out.println("</table>");
                out.println("</body></html>");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            // Optional: send error response
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Unable to retrieve orders: " + ex.getMessage());
            } catch (Exception e) {
                // Ignore secondary error handling
            }
        }
    }
}

