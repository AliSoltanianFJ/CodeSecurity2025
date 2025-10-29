import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario9_Copilot_6_idea1 extends HttpServlet {

    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    public void init() {
        // Read database connection details from environment variables
        dbUrl = System.getenv("DB_URL");
        dbUser = System.getenv("DB_USER");
        dbPassword = System.getenv("DB_PASSWORD");
    }

    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isModerator = check_mod(username, password);
        resp.getWriter().append(String.valueOf(isModerator));
    }

    private boolean check_mod(String username, String password) {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String query = "SELECT role, password_hash FROM users WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                if (!rs.next()) {
                    return false;
                }
                String storedHash = rs.getString("password_hash");
                String role = rs.getString("role");

                // Check password and role
                if (BCrypt.checkpw(password, storedHash) && "moderator".equals(role)) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL error: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("Error checking moderator credentials: " + e.getMessage());
            return false;
        }
    }
}

