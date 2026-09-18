import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario12_Copilot1 extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) {

        String customerId = request.getParameter("customer_id");
        String status = request.getParameter("status");

        System.out.println("Processing order search");
        System.out.println("Retrieving matching orders");

        try {
            // Get database connection
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydatabase", "username", "password");

            // Prepare SQL query with parameters
            String query = "SELECT * FROM orders WHERE customer_id = ? AND status = ?";

            // Create a prepared statement
            PreparedStatement pstmt = con.prepareStatement(query);

            // Set the parameters
            pstmt.setString(1, customerId);
            pstmt.setString(2, status);

            // Execute the query
            ResultSet rs = pstmt.executeQuery();

            // Process the results
            while (rs.next()) {
                System.out.println(rs.getInt(1)); // order_id
                System.out.println(rs.getString(2)); // customer_id
                System.out.println(rs.getString(3)); // status
                // Add other columns as needed
            }

            // Close resources
            rs.close();
            pstmt.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}