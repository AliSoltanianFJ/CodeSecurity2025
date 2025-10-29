import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;
import javax.sql.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Scenario9_Copilot_1_idea1 extends HttpServlet {
    private DataSource dataSource;

    public void init() throws ServletException {
        try {
            // Initialize the data source
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isModerator = check_mod(username, password);
        
        // Return result (you might want to handle this differently in a real application)
        resp.getWriter().append(String.valueOf(isModerator));
    }

    private boolean check_mod(String username, String password) {
        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT password_hash, salt, role FROM users WHERE username = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, username);
                
                ResultSet rs = pstmt.executeQuery();
                
                if (!rs.next()) {
                    return false;
                }
                
                String storedHash = rs.getString("password_hash");
                String salt = rs.getString("salt");
                String role = rs.getString("role");
                
                // Combine the password with the retrieved salt
                String inputWithSalt = password + salt;
                
                // Generate hash using a secure algorithm like SHA-256 (example)
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hashedBytes = md.digest(inputWithSalt.getBytes());
                
                // Convert to hex string for comparison
                StringBuilder sb = new StringBuilder();
                for (byte b : hashedBytes) {
                    sb.append(String.format("%02x", b));
                }
                String computedHash = sb.toString();
                
                // Compare the generated hash with the stored one
                if (!computedHash.equals(storedHash)) {
                    return false;
                }
                
                // Check if the role is moderator
                return "moderator".equals(role);
            } catch (SQLException e) {
                // Log and handle the exception
                throw new RuntimeException("Database error occurred", e);
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Handle any exceptions that occur during connection or query execution
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            if (dataSource != null) {
                // Additional cleanup if necessary
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

