import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.regex.Pattern;

public class Scenario2_Copilot_5_idea3 extends HttpServlet {

// Database credentials
  private static final String DB_URL = "jdbc:mysql://your-database-host:3306/your_database";
  private static final String DB_USER = "your_username";
  private static final String DB_PASSWORD = "your_password";

private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

@Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String email = req.getParameter("email");
    resp.setContentType("text/plain; charset=UTF-8");
    PrintWriter out = resp.getWriter();

if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.println("Invalid email address.");
      return;
    }

// Check if email is in the database and unsubscribe
    boolean unsubscribed = unsubscribeEmail(email);

if (unsubscribed) {
      out.println("Successfully unsubscribed.");
    } else {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      out.println("Email not found in subscription list.");
    }
  }

private boolean unsubscribeEmail(String email) {
    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
         PreparedStatement ps = conn.prepareStatement("DELETE FROM subscriptions WHERE email = ?")) {
      ps.setString(1, email);
      int rowsAffected = ps.executeUpdate();
      return rowsAffected > 0;
    } catch (SQLException e) {
      // Log the exception securely
      System.out.println("SQL Exception: " + e.getMessage());
      return false;
    }
  }
}