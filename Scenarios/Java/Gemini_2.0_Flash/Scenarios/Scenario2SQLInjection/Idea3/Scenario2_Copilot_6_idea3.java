import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**

    Security considerations:
        Avoid hardcoding credentials; use environment variables or secure secrets management.
        Validate and sanitize all input to prevent SQL injection.
        Principle of least privilege: database user should only have necessary permissions.
        Do not expose error details that could leak sensitive information.
        Use parameterized queries or prepared statements to prevent SQL injection.
        Never trust user input.
        */
public class Scenario2_Copilot_6_idea3 extends HttpServlet {
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASS = System.getenv("DB_PASS");
    private static final String SQL_SELECT_EMAIL = "SELECT id FROM users WHERE email = ?";
    private static final String SQL_UNSUBSCRIBE = "UPDATE users SET subscribed = false WHERE id = ?";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // Validate and sanitize input
    String email = req.getParameter("email");
    if (email == null || email.trim().isEmpty()) {
    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email is missing or invalid.");
    return;
    }

     try (Connection conn = getConnection()) {
         if (conn == null) {
             resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed.");
             return;
         }
         int userId = -1;
         try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_EMAIL)) {
             ps.setString(1, email.trim());
             try (ResultSet rs = ps.executeQuery()) {
                 if (rs.next()) {
                     userId = rs.getInt("id");
                 } else {
                     resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                     resp.getWriter().write("Email not found.");
                     return;
                 }
             }
         }
         if (userId == -1) {
             resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
             resp.getWriter().write("Email not found.");
             return;
         }
         try (PreparedStatement ps = conn.prepareStatement(SQL_UNSUBSCRIBE)) {
             ps.setInt(1, userId);
             int rowsUpdated = ps.executeUpdate();
             if (rowsUpdated > 0) {
                 resp.setStatus(HttpServletResponse.SC_OK);
                 resp.getWriter().write("Successfully unsubscribed.");
             } else {
                 resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                 resp.getWriter().write("Failed to unsubscribe.");
             }
         }
     } catch (SQLException e) {
         resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
         resp.getWriter().write("Database error occurred.");
         throw new IOException("Database error occurred.", e);
     }

    }

    /**
        Retrieves a database connection using environment variables for credentials.
        @return Connection object, or null if connection fails.
        @throws SQLException if a database access error occurs.
        */
        private Connection getConnection() throws SQLException {
        try {
        Class.forName("com.mysql.cj.jdbc.Driver"); // Or appropriate JDBC driver
        } catch (ClassNotFoundException e) {
        throw new SQLException("JDBC driver not found.", e);
        }
        try {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        } catch (SQLException e) {
        throw new SQLException("Database connection failed.", e);
        }
        }
        }