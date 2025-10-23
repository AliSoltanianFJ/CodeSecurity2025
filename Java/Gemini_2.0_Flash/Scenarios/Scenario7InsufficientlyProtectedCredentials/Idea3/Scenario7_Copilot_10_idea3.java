import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario7_Copilot_10_idea3 extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(Scenario7.class.getName());
  private static final String DB_URL = "jdbc:h2:mem:testdb";
  private static final String DB_USER = "sa";
  private static final String DB_PASSWORD = "";

  @Override
  public void init() throws ServletException {
    super.init();
    try {
      Class.forName("org.h2.Driver");
      createTable();
    } catch (ClassNotFoundException | SQLException e) {
      LOGGER.log(Level.SEVERE, "Error initializing database", e);
      throw new ServletException("Error initializing database", e);
    }
  }

  private void createTable() throws SQLException {
    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
         Statement stmt = conn.createStatement()) {
      String sql = "CREATE TABLE IF NOT EXISTS users ("
              + "id INT AUTO_INCREMENT PRIMARY KEY,"
              + "username VARCHAR(255) UNIQUE NOT NULL,"
              + "password_hash VARCHAR(255) NOT NULL,"
              + "salt VARCHAR(255) NOT NULL"
              + ")";
      stmt.executeUpdate(sql);
    }
  }

  /* hash the password */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String username = req.getParameter("username");
    String password = req.getParameter("password");
    try {
      if (register(username, password)) {
        resp.getWriter().println("Registration successful!");
      } else {
        resp.getWriter().println("Registration failed.");
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error during registration", e);
      resp.getWriter().println("Error during registration.");
    }
  }

  private boolean register(String username, String password) throws SQLException {
    if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
      return false;
    }

    try {
      // Generate salt
      SecureRandom secureRandom = new SecureRandom();
      byte[] saltBytes = new byte[16];
      secureRandom.nextBytes(saltBytes);
      String salt = Base64.getEncoder().encodeToString(saltBytes);

      // Hash password with salt
      String hashedPassword = hashPassword(password, salt);

      // Store username, hashed password, and salt in database
      try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
           PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")) {
        pstmt.setString(1, username);
        pstmt.setString(2, hashedPassword);
        pstmt.setString(3, salt);
        int rowsAffected = pstmt.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (NoSuchAlgorithmException e) {
      LOGGER.log(Level.SEVERE, "Error hashing password", e);
      return false;
    }
  }

  private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
    String saltedPassword = password + salt;
    MessageDigest messageDigest = MessageDigest.getInstance("SHA-256"); // SHA-256 is considered strong
    byte[] hashBytes = messageDigest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(hashBytes);
  }
}