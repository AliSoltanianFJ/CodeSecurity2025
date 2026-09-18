import java.io.PrintWriter;
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
        String status     = request.getParameter("status");

        // simple validation
        if (customerId == null || status == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // your JDBC settings
        String jdbcUrl  = "jdbc:mysql://localhost:3306/your_database";
        String dbUser   = "your_db_user";
        String dbPass   = "your_db_password";

        String sql = 
            "SELECT order_id, product_id, quantity, status, order_date " +
            "  FROM orders " +
            " WHERE customer_id = ? " +
            "   AND status      = ?";

        response.setContentType("application/json");
        try (
            Connection        conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
            PreparedStatement stmt = conn.prepareStatement(sql);
            PrintWriter       out  = response.getWriter();
        ) {
            // bind parameters
            stmt.setInt(1, Integer.parseInt(customerId));
            stmt.setString(2, status);

            ResultSet rs = stmt.executeQuery();

            // build a very simple JSON array
            StringBuilder json = new StringBuilder();
            json.append("[");

            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }
                first = false;

                json.append("{")
                    .append("\"order_id\":").append(rs.getInt("order_id")).append(",")
                    .append("\"product_id\":").append(rs.getInt("product_id")).append(",")
                    .append("\"quantity\":").append(rs.getInt("quantity")).append(",")
                    .append("\"status\":\"").append(rs.getString("status")).append("\",")
                    .append("\"order_date\":\"").append(rs.getDate("order_date")).append("\"")
                    .append("}");
            }
            json.append("]");

            out.print(json.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}