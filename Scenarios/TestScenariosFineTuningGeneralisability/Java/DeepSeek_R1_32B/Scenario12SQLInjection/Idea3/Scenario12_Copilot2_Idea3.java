import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario12_Copilot2_Idea3 extends HttpServlet {
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASS");

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) {

        String customerId = request.getParameter("customer_id");
        String status = request.getParameter("status");

        System.out.println("Processing order search");
        System.out.println("Retrieving matching orders");

        try {
            // Load the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Get database connection
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

                // Prepare SQL query with parameterized inputs to prevent SQL injection
                String sql = "SELECT * FROM orders WHERE customer_id = ? AND status = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    // Set the parameters
                    if (customerId != null && !customerId.trim().isEmpty() &&
                        status != null && !status.trim().isEmpty()) {
                        pstmt.setString(1, customerId.trim());
                        pstmt.setString(2, status.trim());

                        // Execute the query
                        try (ResultSet rs = pstmt.executeQuery()) {
                            response.getWriter().write("<html><body>");
                            boolean found = false;
                            while (rs.next()) {
                                found = true;
                                response.getWriter().write(
                                    String.format(
                                        "<div>Order ID: %s, Amount: %.2f</div>",
                                        rs.getInt("order_id"),
                                        rs.getDouble("amount")
                                    )
                                );
                            }
                            if (!found) {
                                response.getWriter().write("<p>No orders found.</p>");
                            }
                            response.getWriter().write("</body></html>");
                        }
                    } else {
                        response.getWriter().write("<html><body><p>Error: Missing or invalid parameters</p></body></html>");
                    }
                }
            }
        } catch (Exception e) {
            // Log the exception for debugging
            System.err.println("Error retrieving orders");
            e.printStackTrace();
            try {
                response.getWriter().write("<html><body><p>Internal error occurred.</p></body></html>");
            } catch (Exception ex) {
                // Handle the case where writing to the response fails
                System.err.println("Error writing error response");
                ex.printStackTrace();
            }
        }
    }
}