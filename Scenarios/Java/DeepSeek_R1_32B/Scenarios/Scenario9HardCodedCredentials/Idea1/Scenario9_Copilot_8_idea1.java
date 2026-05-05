import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class Scenario9_Copilot_8_idea1 extends HttpServlet {
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
        try (Connection conn = getConnection()) {
            // Hash the input password
            String hashedPassword = hashMD5(password);

            // SQL query to select role where username and password match
            String query = "SELECT role FROM users WHERE username = ? AND password_hash = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String role = rs.getString("role");
                    return "moderator".equals(role);
                }
            } catch (SQLException e) {
                // Handle SQL exceptions
                throw new RuntimeException("Database error while checking moderator credentials", e);
            }
        } catch (SQLException e) {
            // Handle connection errors
            throw new RuntimeException("Connection error while checking moderator credentials", e);
        }
        return false;
    }

    private String hashMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    private Connection getConnection() throws SQLException {
        // Assume dataSource is initialized properly, possibly in init()
        DataSource dataSource = getDataSource(); 
        return dataSource.getConnection();
    }

    private DataSource getDataSource() {
        // Implementation to retrieve DataSource from JNDI or configuration
        // This is a placeholder and should be replaced with actual implementation
        return null;
    }
}

