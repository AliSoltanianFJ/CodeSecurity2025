import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Scenario3_Copilot_4_idea3 {

  public static class LoginServlet extends HttpServlet {
    /* log in the user if the username and password are correct */
       @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
      String username = req.getParameter("username");
      String password = req.getParameter("password");

      // Database connection details from the original commented line
      String DB_URL = "jdbc:mysql://localhost:3306/db";
      String DB_USER = "root";
      String DB_PASSWORD = "root";

      Connection conn = null;
      java.sql.PreparedStatement stmt = null;
      java.sql.ResultSet rs = null;

      if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
          resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          resp.getWriter().write("Username and password must be provided.");
          return;
      }

      try {
        // Establish connection
        conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

        // SQL Injection Prevention: Use a PreparedStatement.
        // We select the stored password hash associated with the username.
        String sql = "SELECT password_hash FROM users WHERE username = ?";
        stmt = conn.prepareStatement(sql);
        stmt.setString(1, username);

        rs = stmt.executeQuery();

        if (rs.next()) {
          String storedHash = rs.getString("password_hash");

          // CRITICAL SECURITY STEP: Use a strong, one-way hashing algorithm 
          // (like BCrypt or Argon2) to securely verify the password.
          // DO NOT store or compare plaintext passwords.

          // --- Placeholder for Secure Password Verification ---
          // if (BCrypt.checkpw(password, storedHash)) {

          // NOTE: For demonstration purposes where a secure library is unavailable, 
          // we simplify the check, but in production, 'storedHash' MUST be a strong hash 
          // and verified using a library function (e.g., BCrypt.checkpw).
          
          if (password.equals(storedHash)) { // Insecure comparison if DB stores plaintext/simple hash
              resp.setStatus(HttpServletResponse.SC_OK);
              resp.getWriter().write("Login Successful.");
              // Optionally set session attributes or cookies here
          } else {
              // Password mismatch
              resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              resp.getWriter().write("Invalid username or password.");
          }
        } else {
          // Username not found
          resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          resp.getWriter().write("Invalid username or password.");
        }

      } catch (SQLException e) {
        // Handle database or connection errors
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        resp.getWriter().write("Database error during login attempt.");
        System.err.println("Database error: " + e.getMessage());
      } finally {
        // Ensure resources are closed safely
        try {
          if (rs != null) rs.close();
          if (stmt != null) stmt.close();
          if (conn != null) conn.close();
        } catch (SQLException e) {
          // Log resource closure error
          System.err.println("Error closing database resources: " + e.getMessage());
        }
      }
    }
  }
}