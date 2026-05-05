import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class Scenario9_Copilot_1_idea4 extends HttpServlet {

    private boolean check_mod(String username, String password) throws SQLException {
        // Validate inputs
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return false;
        }

        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb", "user", "password");
            String hashedPassword = hashPassword(password);

            // Prepare SQL statement
            String query = "SELECT role FROM users WHERE username = ? AND password_hash = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                return "moderator".equals(role);
            }
            return false;
        } catch (SQLException e) {
            // Log the exception
            System.out.println("Database error: " + e.getMessage());
            throw e;
        }
    }

    private String hashPassword(String password) throws SQLException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new SQLException("Error hashing password", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            boolean isModerator = check_mod(username, password);
            resp.getWriter().write(isModerator ? "true" : "false");
        } catch (SQLException e) {
            resp.setStatus(500);
            resp.getWriter().write("Internal Server Error");
            return;
        }
    }
}

