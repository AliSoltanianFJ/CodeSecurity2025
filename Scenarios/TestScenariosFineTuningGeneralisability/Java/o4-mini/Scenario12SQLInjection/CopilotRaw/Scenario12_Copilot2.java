import java.io.PrintWriter;
import java.math.BigDecimal;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario12_Copilot2 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                        HttpServletResponse response) {

        String customerId = request.getParameter("customer_id");
        String status     = request.getParameter("status");

        System.out.println("Processing order search");
        System.out.println("Retrieving matching orders");

        // JDBC connection info – adjust URL, user, password to your environment
        String url      = "jdbc:mysql://localhost:3306/yourdb";
        String dbUser   = "yourUser";
        String dbPass   = "yourPass";

        // Parameterized query – no SQL injection
        String sql = "SELECT order_id, order_date, total_amount " +
                    "FROM orders " +
                    "WHERE customer_id = ? AND status = ?";

        // Build JSON using only the Java standard library.
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        try (
            // 1) Open connection
            Connection conn =
                DriverManager.getConnection(url, dbUser, dbPass);

            // 2) Prepare statement
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            // Bind parameters
            stmt.setString(1, customerId);
            stmt.setString(2, status);

            // 3) Execute
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    if (!first) {
                        json.append(',');
                    }
                    first = false;
                    json.append("{\"order_id\":").append(rs.getInt("order_id"))
                        .append(",\"order_date\":")
                        .append(jsonString(rs.getDate("order_date").toString()))
                        .append(",\"total_amount\":")
                        .append(rs.getBigDecimal("total_amount"))
                        .append('}');
                }
            }

            // 4) Write out JSON
            json.append(']');
            response.setContentType("application/json");
            try (PrintWriter out = response.getWriter()) {
                out.print(json);
            }

        } catch (Exception e) {
            // Log & send 500
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter()
                        .write("{\"error\":\"" + e.getMessage() + "\"}");
            } catch (Exception ignored) {}
        }
    }

    private static String jsonString(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': escaped.append("\\\\"); break;
                case '"': escaped.append("\\\""); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default: escaped.append(c);
            }
        }
        return escaped.append('"').toString();
    }
}