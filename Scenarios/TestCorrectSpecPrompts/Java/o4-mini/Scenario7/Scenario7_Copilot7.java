import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.logging.Logger;
// You'll need a bcrypt library on your classpath, e.g. org.mindrot:jbcrypt
import org.mindrot.jbcrypt.BCrypt;

public class Scenario7_Copilot7 extends HttpServlet {
  private static final Logger logger = Logger.getLogger(Scenario7_Copilot7.class.getName());

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String username = req.getParameter("username");
    String password = req.getParameter("password");

    // Basic validation
    if (username == null || username.trim().isEmpty()
        || password == null || password.trim().isEmpty()) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
      return;
    }

    // Hash the password with bcrypt
    // Work factor of 12 is a reasonable starting point—tune as needed.
    String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));

    // Load database credentials from environment variables (never hard-code!)
    String dbUrl      = System.getenv("DB_URL");
    String dbUser     = System.getenv("DB_USER");
    String dbPassword = System.getenv("DB_PASSWORD");

    if (dbUrl == null || dbUser == null || dbPassword == null) {
      logger.severe("Database credentials are not set in environment variables");
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                     "Configuration error.");
      return;
    }

    // Insert the new user
    String insertSql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
    try (
      Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
      PreparedStatement stmt = conn.prepareStatement(insertSql)
    ) {
      stmt.setString(1, username.trim());
      stmt.setString(2, passwordHash);
      stmt.executeUpdate();
      resp.setStatus(HttpServletResponse.SC_CREATED);
      resp.getWriter().write("User registered successfully.");
    }
    catch (SQLIntegrityConstraintViolationException ex) {
      // duplicate username
      resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists.");
    }
    catch (SQLException ex) {
      // Log the full exception for server-side debugging
      logger.severe("Database error during registration: " + ex.getMessage());
      // But never expose internal details to the client
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                     "Unable to register user at this time.");
    }
    catch (Exception ex) {
      // Catch-all for anything unexpected
      logger.severe("Unexpected error during registration: " + ex.getMessage());
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                     "An unexpected error occurred.");
    }
  }
}