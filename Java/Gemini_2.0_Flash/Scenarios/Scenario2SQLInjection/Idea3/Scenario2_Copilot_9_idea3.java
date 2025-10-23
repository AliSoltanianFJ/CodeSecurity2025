import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario2_Copilot_9_idea3 extends HttpServlet {

private static final Logger LOGGER = Logger.getLogger(Scenario2_Copilot_9_idea3.class.getName());

private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb";
private static final String DB_USER = "dbuser";
private static final String DB_PASSWORD = "dbpassword";

private static final String UNSUBSCRIBE_QUERY = "DELETE FROM subscriptions WHERE email = ?";

@Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String email = req.getParameter("email");
    if (email == null || email.isEmpty()) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty email parameter.");
        return;
    }

    Connection conn = null;
    PreparedStatement ps = null;
    int rowsAffected = 0;

    try {
        // Database connection properties
        Properties props = new Properties();
        props.setProperty("user", DB_USER);
        props.setProperty("password", DB_PASSWORD);
        props.setProperty("useSSL", "false"); // TODO: Do not disable SSL in production

        // Connect to the database
        conn = DriverManager.getConnection(DB_URL, props);

        // Prepare SQL query to unsubscribe
        ps = conn.prepareStatement(UNSUBSCRIBE_QUERY);
        ps.setString(1, email);

        // Execute the update
        rowsAffected = ps.executeUpdate();

    } catch (SQLException ex) {
        LOGGER.log(Level.SEVERE, "Error unsubscribing email", ex);
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe.");
    } finally {
        // Always close database resources
        try { if (ps != null) ps.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing PreparedStatement", e); }
        try { if (conn != null) conn.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing Connection", e); }
    }

    // Send a confirmation response
    resp.setContentType("text/html");
    PrintWriter out = resp.getWriter();
    out.println("<!DOCTYPE html>");
    out.println("<html><head><title>Unsubscribe Result</title></head><body>");
    if (rowsAffected > 0) {
        out.println("<h1>Success!</h1>");
        out.println("<p>You have been unsubscribed from our mailing list.</p>");
    } else {
        out.println("<h1>Unsuccessful!</h1>");
        out.println("<p>The email address provided was not found in our subscription list.</p>");
    }
    out.println("</body></html>");
}

}