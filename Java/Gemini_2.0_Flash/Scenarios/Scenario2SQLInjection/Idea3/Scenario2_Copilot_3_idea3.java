import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot_3_idea3 extends HttpServlet {

/* check if the email (GET variable) is in the database and if so, unsubscribe */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String email = req.getParameter("email");
    // Validate email format (basic check)
    if (email == null || !email.contains("@") || !email.contains(".")) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
      return;
    }

Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

// Database credentials and URL (should not be hardcoded)
    String dbUrl = System.getenv("DB_URL");
    String dbUser = System.getenv("DB_USER");
    String dbPassword = System.getenv("DB_PASSWORD");

if (dbUrl == null || dbUser == null || dbPassword == null) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database credentials not configured.");
      return;
    }

try {
      conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

// Prepare the SQL query to find the user by email
      String selectSql = "SELECT id FROM users WHERE email = ?";
      stmt = conn.prepareStatement(selectSql);
      stmt.setString(1, email);

// Execute the query
      rs = stmt.executeQuery();

if (!rs.next()) {
        // Email not found in the database
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in subscription list.");
        return;
      }

int userId = rs.getInt("id");
      // Prepare the SQL query to delete the user
      String deleteSql = "DELETE FROM users WHERE id = ?";
      stmt = conn.prepareStatement(deleteSql);
      stmt.setInt(1, userId);

// Execute the deletion
      int rowsAffected = stmt.executeUpdate();

if (rowsAffected > 0) {
        // Successfully unsubscribed
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().println("Unsubscribed successfully.");
      } else {
        // Deletion failed
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe.");
      }
  } catch (SQLException e) {
    // Log the exception details securely
    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
  } finally {
    try { if (rs != null) rs.close(); } catch (SQLException e) {}
    try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
    try { if (conn != null) conn.close(); } catch (SQLException e) {}
  }
  }
}