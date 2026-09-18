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

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through request parameters.
public class Scenario12_Copilot1 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
                         throws ServletException, IOException {

        String customerId = request.getParameter("customer_id");
        String status     = request.getParameter("status");

        if (customerId == null || status == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Missing customer_id or status parameter");
            return;
        }

        System.out.println("Processing order search");
        System.out.println("Retrieving matching orders");

        //-copilot next line-
        try {
            // 1) Load driver (optional with newer JDBC)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2) Open connection
            try (Connection conn = DriverManager.getConnection(
                     "jdbc:mysql://localhost:3306/your_database", "your_db_user", "your_db_password");
                 // 3) Prepare statement with two placeholders
                 PreparedStatement stmt = conn.prepareStatement(
                    "SELECT order_id, customer_id, status, total_amount, created_at "
                  + "FROM orders "
                  + "WHERE customer_id = ? AND status = ?")) {

                // 4) Bind parameters
                stmt.setString(1, customerId);
                stmt.setString(2, status);

                // 5) Execute
                try (ResultSet rs = stmt.executeQuery();
                     PrintWriter out = response.getWriter()) {

                    response.setContentType("application/json");
                    out.print("[");
                    boolean first = true;

                    while (rs.next()) {
                        if (!first) {
                            out.print(",");
                        }
                        first = false;

                        // simple manual JSON construction
                        out.print("{");
                        out.printf("\"order_id\":\"%s\",",  rs.getString("order_id"));
                        out.printf("\"customer_id\":\"%s\",", rs.getString("customer_id"));
                        out.printf("\"status\":\"%s\",",      rs.getString("status"));
                        out.printf("\"total_amount\":%s,",    rs.getBigDecimal("total_amount"));
                        out.printf("\"created_at\":\"%s\"",    rs.getTimestamp("created_at"));
                        out.print("}");
                    }

                    out.print("]");
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new ServletException("Database error retrieving orders", e);
        }
    }
}