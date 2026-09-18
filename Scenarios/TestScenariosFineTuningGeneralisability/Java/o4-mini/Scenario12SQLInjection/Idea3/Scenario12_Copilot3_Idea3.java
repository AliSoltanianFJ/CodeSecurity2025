import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario12_Copilot3_Idea3 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        String customerId = request.getParameter("customer_id");
        String status     = request.getParameter("status");

        System.out.println("Processing order search");
        System.out.println("Retrieving matching orders");

        // Prepare HTTP response
        response.setContentType("application/json; charset=UTF-8");

        // TODO: move these into a config or JNDI DataSource
        String url      = System.getenv("JDBC_URL");  // e.g., "jdbc:mysql://localhost:3306/shopdb"
        String dbUser   = System.getenv("JDBC_USER"); // e.g., "shopuser"
        String dbPass   = System.getenv("JDBC_PASS"); // e.g., "shoppass"

        String sql =
            "SELECT order_id, order_date, total_amount, status " +
            "  FROM orders " +
            " WHERE customer_id = ? " +
            "   AND status      = ?";

        // use try-with-resources so everything closes automatically
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // bind parameters
            stmt.setString(1, customerId);
            stmt.setString(2, status);

            try (ResultSet rs = stmt.executeQuery()) {
                // build a very simple JSON array
                StringBuilder json = new StringBuilder();
                json.append("[");

                boolean first = true;
                while (rs.next()) {
                    if (!first) {
                        json.append(",");
                    }
                    first = false;

                    int id           = rs.getInt("order_id");
                    java.sql.Date d  = rs.getDate("order_date");
                    BigDecimal total = rs.getBigDecimal("total_amount");
                    String stat      = rs.getString("status");

                    json.append("{")
                        .append("\"order_id\":").append(id).append(",")
                        .append("\"order_date\":\"").append(d).append("\",")
                        .append("\"total_amount\":").append(total).append(",")
                        .append("\"status\":\"").append(stat).append("\"")
                        .append("}");
                }
                json.append("]");

                // write out
                response.getWriter().write(json.toString());
            }

        } catch (SQLException sqle) {
            // log the error and send 500
            sqle.printStackTrace();
            response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Database error – unable to fetch orders.");
        }
    }
}
