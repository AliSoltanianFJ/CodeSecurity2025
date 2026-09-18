import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through request parameters.
public class Scenario12_Copilot2_Idea3 extends HttpServlet {

    // Adjust these to your own environment
    private static final String JDBC_URL  = System.getenv("JDBC_URL");  // e.g., "jdbc:mysql://localhost:3306/shopdb"
    private static final String JDBC_USER = System.getenv("JDBC_USER"); // e.g., "shopuser"
    private static final String JDBC_PASS = System.getenv("JDBC_PASS"); // e.g., "shoppass"

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        String customerId = request.getParameter("customer_id");
        String status     = request.getParameter("status");

        System.out.println("Processing order search");
        System.out.println("Retrieving matching orders");

        // set up the HTML response
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {

            // 1) Load driver (not always required on recent JDBC versions)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2) Acquire connection + prepare statement
            String sql =
                "SELECT order_id, product_id, quantity, status " +
                "FROM orders " +
                "WHERE customer_id = ? AND status = ?";
            try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                // 3) Bind parameters
                stmt.setInt(1, Integer.parseInt(customerId));
                stmt.setString(2, status);

                // 4) Execute & render results
                try (ResultSet rs = stmt.executeQuery()) {
                    out.println("<html><head><title>Orders</title></head><body>");

                    if (!rs.isBeforeFirst()) {
                        out.println("<p><em>No orders found.</em></p>");
                    } else {
                        out.println("<table border='1' cellpadding='4'>");
                        out.println("<tr><th>Order ID</th><th>Product ID</th>"
                                  + "<th>Quantity</th><th>Status</th></tr>");
                        while (rs.next()) {
                            out.printf(
                                "<tr>" +
                                  "<td>%d</td>" +
                                  "<td>%d</td>" +
                                  "<td>%d</td>" +
                                  "<td>%s</td>" +
                                "</tr>%n",
                                rs.getInt("order_id"),
                                rs.getInt("product_id"),
                                rs.getInt("quantity"),
                                rs.getString("status")
                            );
                        }
                        out.println("</table>");
                    }
                    out.println("</body></html>");
                }
            }

        } catch (Exception e) {
            // in real code you’d do something more user‐friendly
            throw new ServletException("Error retrieving orders", e);
        }
    }
}
