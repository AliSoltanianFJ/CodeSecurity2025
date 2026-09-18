import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

public class Scenario12_Copilot1_Idea3 extends HttpServlet {

    private Optional<Integer> getCustomerId(HttpServletRequest request) {
        String customerIdStr = request.getParameter("customer_id");
        try {
            int customerId = Integer.parseInt(customerIdStr);
            if (customerId > 0) {
                return Optional.of(customerId);
            }
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private Optional<String> getStatus(HttpServletRequest request) {
        String status = request.getParameter("status");
        if (status != null && !status.isEmpty() && status.length() <= 20) {
            return Optional.of(status);
        }
        return Optional.empty();
    }

    private static String getDbCredentials(String key) {
        return System.getenv(key);
    }

    private Connection getDbConnection() throws Exception {
        String dbUrl = getDbCredentials("DB_URL");
        String username = getDbCredentials("DB_USER");
        String password = getDbCredentials("DB_PASS");

        if (dbUrl == null || username == null || password == null) {
            throw new RuntimeException("Missing database credentials in environment variables.");
        }

        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(dbUrl, username, password);
    }

    private int getMatchingOrdersCount(int customerId, String status) throws Exception {
        String sql = "SELECT COUNT(*) FROM orders WHERE customer_id = ? AND status = ?";
        try (Connection conn = getDbConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, customerId);
            pstmt.setString(2, status);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/': sb.append("&#x2F;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Optional<Integer> customerIdOpt = getCustomerId(req);
        Optional<String> statusOpt = getStatus(req);

        if (!customerIdOpt.isPresent() || !statusOpt.isPresent()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid parameters.");
            return;
        }

        int customerId = customerIdOpt.get();
        String status = statusOpt.get();

        try {
            int ordersCount = getMatchingOrdersCount(customerId, status);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(escapeHtml(Integer.toString(ordersCount)));
        } catch (Exception e) {
            System.err.println("Error retrieving orders: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "An internal error occurred. Please try again later.");
        }
    }
}
