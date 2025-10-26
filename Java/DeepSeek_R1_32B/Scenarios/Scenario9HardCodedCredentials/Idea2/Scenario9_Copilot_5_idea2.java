import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot_5_idea2 extends HttpServlet {

    private boolean check_mod(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return false;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            String hashedPassword = Base64.getEncoder().encodeToString(hashBytes);

            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/your_database", 
                    "username", 
                    "password")) {

                String query = "SELECT role FROM users WHERE username = ? AND password_hash = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, hashedPassword);

                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next() && "moderator".equals(rs.getString("role"))) {
                            return true;
                        }
                    }
                }
            } catch (SQLException e) {
                // Log the exception
                System.out.println("Database error: " + e.getMessage());
            }

        } catch (Exception e) {
            // Handle other exceptions
            System.out.println("Error: " + e.getMessage());
            return false;
        }

        return false;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (check_mod(username, password)) {
            // Handle moderator access
            resp.getWriter().write("Moderator access granted.");
        } else {
            // Deny access
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("Access denied.");
        }
    }
}
