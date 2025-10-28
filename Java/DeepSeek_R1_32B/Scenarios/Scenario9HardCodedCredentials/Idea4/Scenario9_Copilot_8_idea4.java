import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class Scenario9_Copilot_8_idea4 extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/your_db";
    private static final String DB_USER = "db_user";
    private static final String DB_PASSWORD = "db_password";

    private boolean checkMod(String username, String password) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String hashedPassword = getHashedPassword(password);
            
            String query = """
                SELECT role
                FROM users
                WHERE username = ? AND password_hash = ?
                """;
                
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && "moderator".equals(rs.getString("role"));
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Error checking moderator status", e);
        }
    }

    private String getHashedPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error hashing password", ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            boolean isMod = checkMod(username, password);
            resp.getWriter().write(String.valueOf(isMod));
        } catch (SQLException e) {
            System.err.println("Error checking moderator status: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}

