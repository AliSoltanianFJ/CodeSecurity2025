import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario7_Copilot_9 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        
        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password are required.");
            return;
        }

        try {
            // Hash the password using SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            String hashedPassword = bytesToHex(hashBytes);

            // Connect to the database
            Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:testdb", "sa", "");
            
            try {
                // Check if username already exists
                PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM users WHERE username = ?");
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                rs.next();
                int count = rs.getInt(1);

                if (count > 0) {
                    resp.setStatus(HttpServletResponse.SC_CONFLICT);
                    resp.getWriter().write("Username already exists.");
                    return;
                }

                // Insert new user
                pstmt = conn.prepareStatement(
                    "INSERT INTO users (username, password_hash) VALUES (?, ?)");
                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);
                pstmt.executeUpdate();

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Registration successful.");

            } finally {
                // Close the database connection
                if (conn != null) {
                    conn.close();
                }
            }

        } catch (SQLException e) {
            // Handle database errors
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database error: " + e.getMessage());
        } catch (Exception e) {
            // Handle other exceptions
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error: " + e.getMessage());
        }
    }

    private String bytesToHex(byte[] hashBytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

