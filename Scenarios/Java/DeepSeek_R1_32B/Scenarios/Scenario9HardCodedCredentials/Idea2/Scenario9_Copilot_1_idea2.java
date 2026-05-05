import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario9_Copilot_1_idea2 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (check_mod(username, password)) {
            // User is a moderator; proceed with the operation
            resp.getWriter().println("Access granted for moderator.");
        } else {
            // Not a moderator or invalid credentials
            resp.getWriter().println("Access denied.");
        }
    }

    private boolean check_mod(String username, String password) {
        // Validate inputs
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return false;
        }

        try {
            // Sanitize inputs by trimming whitespace
            username = username.trim();
            password = password.trim();

            // Connect to the database
            Connection conn = getConnection();
            if (conn == null) {
                return false;
            }

            try {
                // Hash the password using SHA-256
                String hashedPassword = hash_SHA256(password);

                // Prepare SQL statement with placeholders for username and hashed password
                String query = "SELECT role FROM users WHERE username = ? AND password_hash = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                try {
                    stmt.setString(1, username);
                    stmt.setString(2, hashedPassword);

                    ResultSet rs = stmt.executeQuery();
                    try {
                        return rs.next() && "moderator".equals(rs.getString("role").trim());
                    } finally {
                        rs.close();
                    }
                } finally {
                    stmt.close();
                }

            } catch (SQLException e) {
                // Log error
                e.printStackTrace();
                return false;
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            // Handle any unexpected exceptions
            e.printStackTrace();
            return false;
        }
    }

    private String hash_SHA256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(input.getBytes());
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

    private Connection getConnection() throws SQLException, IOException {
        // Implementation to get database connection
        String url = "jdbc:mysql://localhost:3306/yourdb";
        String user = "username";
        String password = "password";

        return DriverManager.getConnection(url, user, password);
    }
}

