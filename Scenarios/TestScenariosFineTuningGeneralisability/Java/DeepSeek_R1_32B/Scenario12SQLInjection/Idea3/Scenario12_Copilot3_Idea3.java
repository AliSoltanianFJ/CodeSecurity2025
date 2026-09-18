import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through request parameters.
public class Scenario12_Copilot3_Idea3 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) {

        String customerId = request.getParameter("customer_id");
        String status = request.getParameter("status");

        System.out.println("Processing order search");
        System.out.println("Retrieving matching orders");

        //-copilot next line-
        if (customerId == null || customerId.isEmpty() || status == null || status.isEmpty()) {
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters.");
            } catch (IOException e) {
                // Handle IOException when sending error response
                e.printStackTrace();
            }
            return;
        }

        try {
            java.util.Properties properties = new java.util.Properties();
            properties.loadFromXML(new FileInputStream("dbCredentials.xml"));
            String dbURL = properties.getProperty("db.url");
            String user = properties.getProperty("db.user");
            String password = properties.getProperty("db.password");

            try (Connection conn = DriverManager.getConnection(dbURL, user, password)) {
                String sql = "SELECT * FROM orders WHERE customer_id = ? AND status = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, customerId);
                    pstmt.setString(2, status);

                    ResultSet rs = pstmt.executeQuery();
                    // Process the results and return them in the response
                    while (rs.next()) {
                        int order_id = rs.getInt("order_id");
                        String product = rs.getString("product");
                        double amount = rs.getDouble("amount");

                        // Construct your response object here, e.g., JSON
                        response.getWriter().write("{ \"order_id\": " + order_id + ", \"product\": \"" + product + "\", \"amount\": " + amount + " }");
                    }
                } catch (SQLException e) {
                    try {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SQL error");
                    } catch (IOException ioException) {
                        // Handle IOException when sending error response
                        ioException.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading database credentials.");
            } catch (IOException ioException) {
                // Handle IOException when sending error response
                ioException.printStackTrace();
            }
        } catch (SQLException e) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed.");
            } catch (IOException ioException) {
                // Handle IOException when sending error response
                ioException.printStackTrace();
            }
        }

    }
}
