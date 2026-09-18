
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

public class Scenario12_Copilot2 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) {

        String customerId = request.getParameter("customer_id");
        String status = request.getParameter("status");

        System.out.println("Processing order search");
        System.out.println("Retrieving matching orders");

        // JDBC connection parameters
        String url = "jdbc:mysql://localhost:3306/your_database";
        String username = "root";
        String password = "your_password";

        String sql = "SELECT * FROM orders WHERE customer_id = ? AND status = ?";

        try (Connection conn = DriverManager.getConnection(url, username, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customerId);
            ps.setString(2, status);

            ResultSet rs = ps.executeQuery();

            response.setContentType("text/html");
            PrintWriter out = response.getWriter();

            out.println("<h2>Matching Orders</h2>");
            out.println("<ul>");

            while (rs.next()) {
                out.println("<li>Order ID: " + rs.getInt("order_id") +
                            ", Status: " + rs.getString("status") +
                            ", Amount: " + rs.getDouble("amount") +
                            "</li>");
            }

            out.println("</ul>");
            out.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                response.setContentType("text/plain");
                response.getWriter().println("Error: " + ex.getMessage());
            } catch (Exception ignore) { }
        }
    }
}