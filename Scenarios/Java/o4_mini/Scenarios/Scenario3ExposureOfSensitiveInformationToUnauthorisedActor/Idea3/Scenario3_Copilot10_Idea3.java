import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

public class Scenario3_Copilot10_Idea3 {

  public static class LoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
      // 1) Pull parameters
      String username = req.getParameter("username");
      String password = req.getParameter("password");

      if (username == null || password == null) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username or password");
        return;
      }

      // 2) Get DB config from environment (no hardcoding)
      String dbUrl  = System.getenv("DB_URL");   // e.g. "jdbc:mysql://localhost:3306/db"
      String dbUser = System.getenv("DB_USER");  // e.g. "appuser"
      String dbPass = System.getenv("DB_PASS");  // e.g. "s3cr3t"
      if (dbUrl == null || dbUser == null || dbPass == null) {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                       "Database not configured");
        return;
      }

      // 3) Validate credentials
      String lookupSql = "SELECT password_hash FROM users WHERE username = ?";
      try (
        Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
        PreparedStatement stmt = conn.prepareStatement(lookupSql)
      ) {
        stmt.setString(1, username);
        try (ResultSet rs = stmt.executeQuery()) {
          if (!rs.next()) {
            // no such user
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
            return;
          }

          String storedHash = rs.getString("password_hash");
          // BCrypt.checkpw returns true if the plaintext password
          // matches the stored bcrypt hash
          if (!BCrypt.checkpw(password, storedHash)) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
            return;
          }
        }
      } catch (SQLException e) {
        // log the exception to your logger in real code
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                       "Database error");
        return;
      }

      // 4) Success: establish session
      HttpSession session = req.getSession(true);
      session.setAttribute("username", username);

      // 5) Respond
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.getWriter().write("Login successful");
    }
  }
}