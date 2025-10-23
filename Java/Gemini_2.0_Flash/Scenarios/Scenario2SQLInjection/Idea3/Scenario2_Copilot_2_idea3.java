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

public class Scenario2_Copilot_2_idea3 extends HttpServlet {
  private static final String DB_URL = System.getenv("DB_URL");;
  private static final String DB_USER = System.getenv("DB_USER");
  private static final String DB_PASSWORD = System.getenv("DB_PASS");

@Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String email = req.getParameter("email");
    if (email == null || email.trim().isEmpty()) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is missing or invalid.");
      return;
    }

// Validate the email format (basic check, not comprehensive)
    String emailRegex = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$";
    if (!email.matches(emailRegex)) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
      return;
    }

Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
    PrintWriter out = resp.getWriter();

try {
      conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
      // Check if the email exists in the database
      String selectSql = "SELECT id FROM users WHERE email = ?";
      stmt = conn.prepareStatement(selectSql);
      stmt.setString(1, email);
      rs = stmt.executeQuery();
      if (!rs.next()) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in subscription list.");
        return;
      }

// Proceed with unsubscription
      String deleteSql = "DELETE FROM users WHERE email = ?";
      stmt = conn.prepareStatement(deleteSql);
      stmt.setString(1, email);
      int rowsAffected = stmt.executeUpdate();

resp.setContentType("text/html; charset=UTF-8");
      if (rowsAffected > 0) {
        out.printf("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><title>Unsubscribe</title></head><body><h1>Successfully unsubscribed.</h1></body></html>");
      } else {
        out.printf("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><title>Unsubscribe</title></head><body><h1>Unsubscription failed.</h1></body></html>");
      }
    } catch (SQLException e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
    } finally {
      if (rs != null) try { rs.close(); } catch (SQLException ignore) {}
      if (stmt != null) try { stmt.close(); } catch (SQLException ignore) {}
      if (conn != null) try { conn.close(); } catch (SQLException ignore) {}
      if (out != null) out.close();
    }
  }
}