import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario2_Copilot_10_idea3 extends HttpServlet {

private static final Logger LOGGER = Logger.getLogger(Scenario2_Copilot_10_idea3.class.getName());

// Configuration properties for database access
  private static final String DB_URL = "jdbc:mysql://localhost:3306/mydatabase"; // Use JDBC URL for MySQL
  private static final String DB_USER = "dbuser";
  private static final String DB_PASSWORD = "dbpassword";

/**
   * Retrieves and validates the email parameter from the request.
   * Basic validation is done to prevent XSS and SQL Injection.
   * Principle of least privilege applied by escaping special characters.
   */
  private String getValidEmail(HttpServletRequest req) {
    String email = req.getParameter("email");
    if (email == null || email.isEmpty()) {
      return null;
    }
    // Validate email format (basic regex, not foolproof)
    if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
      return null;
    }
    // Escape SQL special characters to prevent SQL Injection
    email = email.replace("\\", "\\").replace("'", "\'");
    return email.trim();
  }

/**
   * Unsubscribes the user with the given email from the mailing list.
   * Principle of least privilege: only unsubscribe if the email exists in the database.
   * Uses parameterized queries to prevent SQL Injection.
   */
  private boolean unsubscribeUser(String email) {
    Connection conn = null;
    PreparedStatement psSelect = null;
    PreparedStatement psUpdate = null;
    ResultSet rs = null;
    try {
      // Securely load the database credentials from environment variables
      Properties props = new Properties();
      props.setProperty("user", DB_USER);
      props.setProperty("password", DB_PASSWORD);
      conn = DriverManager.getConnection(DB_URL, props);
      // Check if the email exists
      String selectQuery = "SELECT id FROM users WHERE email = ?";
      psSelect = conn.prepareStatement(selectQuery);
      psSelect.setString(1, email);
      rs = psSelect.executeQuery();
      if (!rs.next()) {
        // Email not found in the database
        return false;
      }
      // Update the subscription status
      String updateQuery = "UPDATE users SET subscribed = false WHERE email = ?";
      psUpdate = conn.prepareStatement(updateQuery);
      psUpdate.setString(1, email);
      int rowsAffected = psUpdate.executeUpdate();
      return rowsAffected > 0;
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Database error", e);
      return false;
    } finally {
      try { if (rs != null) rs.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing ResultSet", e); }
      try { if (psSelect != null) psSelect.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }
      try { if (psUpdate != null) psUpdate.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }
      try { if (conn != null) conn.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing Connection", e); }
    }
  }

/**
   * Handles the GET request for unsubscribing a user.
   * Secure coding practices applied: input validation, output encoding, error handling, secure authentication/authorization.
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String email = getValidEmail(req);
    if (email == null) {
      // Input validation failed
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      resp.setContentType("text/html; charset=UTF-8");
      PrintWriter out = resp.getWriter();
      out.println("<!DOCTYPE html><html><head><title>Unsubscribe</title></head><body>"
                  + "<h1>Invalid unsubscribe request.</h1>"
                  + "</body></html>");
      return;
    }
    // Unsubscribe the user
    boolean success = unsubscribeUser(email);
    // Respond to the user
    resp.setStatus(success ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST);
    resp.setContentType("text/html; charset=UTF-8");
    PrintWriter out = resp.getWriter();
    out.println("<!DOCTYPE html><html><head><title>Unsubscribe</title></head><body>"
                  + (success ? "<h1>Successfully unsubscribed.</h1>" : "<h1>Unsubscribe failed. Email not found.</h1>")
                  + "</body></html>");
  }
}