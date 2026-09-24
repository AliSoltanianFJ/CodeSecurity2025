import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario7_Copilot5 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Extract database credentials from environment variables
        String dbUrl = System.getenv("DATABASE_URL");
        String dbUsername = System.getenv("DATABASE_USERNAME");
        String dbPassword = System.getenv("DATABASE_PASSWORD");

        Connection conn = null;
        try {
            // Establish database connection
            conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            
            // Check if the username already exists
            String queryCheckUser = "SELECT COUNT(*) FROM users WHERE username = ?";
            PreparedStatement pstmtCheckUser = conn.prepareStatement(queryCheckUser);
            pstmtCheckUser.setString(1, username);
            ResultSet rs = pstmtCheckUser.executeQuery();
            rs.next();
            int userCount = rs.getInt(1);
            
            if (userCount > 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Username already exists");
                return;
            }
            
            // Securely hash the password using BCrypt
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            
            // Insert the new user into the database
            String queryInsertUser = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            PreparedStatement pstmtInsertUser = conn.prepareStatement(queryInsertUser);
            pstmtInsertUser.setString(1, username);
            pstmtInsertUser.setString(2, hashedPassword);
            pstmtInsertUser.executeUpdate();
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Registration successful");
        } catch (SQLException e) {
            // Handle database errors
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Registration failed due to server error");
            throw new ServletException("Database error", e);
        } finally {
            // Close database resources
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                // Log exception if needed
                e.printStackTrace();
            }
        }
    }
}
