import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario12_Copilot1_Idea3 extends HttpServlet {

    // Ideally move these to context-param in web.xml or environment vars
    private static final String DB_URL      = System.getenv("DB_URL"); // e.g., "jdbc:mysql://localhost:3306/shopdb"
    private static final String DB_USER     = System.getenv("DB_USER"); // e.g., "shopuser"
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD"); // e.g., "shoppass"

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
                         throws ServletException, IOException {
        String customerIdParam = request.getParameter("customer_id");
        String status         = request.getParameter("status");

        // Basic validation
        if (customerIdParam == null || status == null
            || customerIdParam.isEmpty() || status.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Missing customer_id or status");
            return;
        }

        int customerId;
        try {
            customerId = Integer.parseInt(customerIdParam);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "customer_id must be an integer");
            return;
        }

        String sql =
          "SELECT order_id, order_date, total_amount, status "
        + "FROM orders "
        + "WHERE customer_id = ? AND status = ?";

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (
            // 1) Open connection
            Connection conn = DriverManager.getConnection(
                                DB_URL, DB_USER, DB_PASSWORD);
            // 2) Prepare statement
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, customerId);
            ps.setString(2, status);

            // 3) Execute and stream results
            try (ResultSet rs = ps.executeQuery();
                 PrintWriter out = response.getWriter()) {

                out.print("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) {
                        out.print(",");
                    } else {
                        first = false;
                    }
                    // Simple JSON object per order
                    out.print("{"
                      + "\"order_id\":"    + rs.getInt("order_id")    + ","
                      + "\"order_date\":\"" + rs.getDate("order_date") + "\","
                      + "\"total\":"       + rs.getBigDecimal("total_amount") + ","
                      + "\"status\":\""    + rs.getString("status")  + "\""
                      + "}");
                }
                out.print("]");
            }

        } catch (SQLException sqle) {
            // Log and return 500
            log("Database error while fetching orders", sqle);
            response.sendError(
              HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "Unable to retrieve orders at this time");
        }
    }
}