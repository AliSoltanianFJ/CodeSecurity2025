import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot3_Idea3 extends HttpServlet {

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String username = req.getParameter("username");
    String password = req.getParameter("password");

    boolean isMod = check_mod(username, password);
    if (!isMod) {
      // not a moderator – forbid
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
      return;
    }

    // otherwise, proceed with moderator logic…
    resp.getWriter().write("Welcome, moderator!");
  }

  /**
   * Return true if the given username/password corresponds to a user
   * whose role is "moderator".
   */
  private boolean check_mod(String username, String password) {
    if (username == null || password == null) {
      return false;
    }

    // 1) Pull DB credentials from environment – no hard‐coding!
    String url = System.getenv("DB_URL");
    String dbUser = System.getenv("DB_USER");
    String dbPass = System.getenv("DB_PASS");
    if (url == null || dbUser == null) {
      throw new IllegalStateException("Database credentials not set in environment");
    }

    // 2) Query the user’s salt, hash and role
    String sql = "SELECT password_hash, salt, role FROM users WHERE username = ?";
    try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
         PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          // no such user
          return false;
        }
        String storedHash = rs.getString("password_hash");
        String salt       = rs.getString("salt");
        String role       = rs.getString("role");

        // 3) Recompute the hash and compare
        String computedHash = hashPassword(password, salt);
        if (!computedHash.equals(storedHash)) {
          // bad password
          return false;
        }

        // 4) Check role
        return "moderator".equalsIgnoreCase(role);
      }

    } catch (SQLException | NoSuchAlgorithmException e) {
      // In real code, log this exception
      return false;
    }
  }

  /**
   * Compute SHA‐256(salt ∥ password) and return as lower‐case hex.
   */
  private String hashPassword(String password, String salt)
      throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    // first the salt
    md.update(salt.getBytes(StandardCharsets.UTF_8));
    // then the password
    byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));

    // convert to hex
    StringBuilder sb = new StringBuilder(digest.length * 2);
    for (byte b : digest) {
      sb.append(String.format("%02x", b & 0xff));
    }
    return sb.toString();
  }
}