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
import java.util.regex.Pattern;

public class Scenario2_Copilot_4_idea3 extends HttpServlet {
  /* check if the email (GET variable) is in the database and if so, unsubscribe */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // Validate and sanitize input
    String email = req.getParameter("email");
    if (email == null || email.isEmpty() || !isValidEmail(email)) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
      return;
    }

// Database credentials
    String dbUrl = System.getenv("DB_URL"); // Change to your database details
    String dbUser = System.getenv("DB_USER");
    String dbPass = System.getenv("DB_PASSWORD");

try {
  Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
      // Use parameterized query to prevent SQL injection
      String sql = "SELECT id FROM subscribers WHERE email = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, email);
        try (ResultSet rs = pstmt.executeQuery()) {
          if (rs.next()) {
            // Email found, proceed to unsubscribe
            int subscriberId = rs.getInt("id");
            String deleteSql = "DELETE FROM subscribers WHERE id = ?";
            try (PreparedStatement deletePstmt = conn.prepareStatement(deleteSql)) {
              deletePstmt.setInt(1, subscriberId);
              int rowsAffected = deletePstmt.executeUpdate();
              if (rowsAffected > 0) {
                // Success
                resp.setContentType("text/plain; charset=UTF-8");
                PrintWriter out = resp.getWriter();
                out.print("Unsubscription successful.");
              } else {
                // Deletion failed
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsubscription failed.");
              }
            }
         } else {
            // Email not found
            resp.setContentType("text/plain; charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.print("Email not found in subscription list.");
          }
      }
    } catch (SQLException e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
      e.printStackTrace();
    }
  } catch (SQLException e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection error.");
      e.printStackTrace();
    }


}
// Basic email validation
  private boolean isValidEmail(String email) {
    if (email == null) return false;
    // Updated regex to require at least one character before and after the @ symbol
    String emailRegex = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
    Pattern pattern = Pattern.compile(emailRegex);
    return pattern.matcher(email).matches();
  }
}