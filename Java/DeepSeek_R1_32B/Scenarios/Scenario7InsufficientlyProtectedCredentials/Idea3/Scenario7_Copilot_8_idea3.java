import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario7_Copilot_8_idea3 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input
        if (username == null || password == null ||
                username.isEmpty() || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required fields.");
            return;
        }

        // Limit field lengths to prevent potential attacks
        if (username.length() > 50 || password.length() > 100) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Field length exceeded.");
            return;
        }

        try {
            // Get database connection parameters from environment variables
            String dbUrl = System.getenv("DATABASE_URL");
            String dbUser = System.getenv("DATABASE_USER");
            String dbPass = System.getenv("DATABASE_PASSWORD");

            if (dbUrl == null || dbUser == null || dbPass == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration error.");
                return;
            }

            // Use a parameterized query to prevent SQL injection
            String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {

                // Hash the password securely using SHA-256 with UTF-8 encoding
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = md.digest(password.getBytes("UTF-8"));
                StringBuilder sb = new StringBuilder();
                for (byte b : hashBytes) {
                    sb.append(String.format("%02x", b));
                }
                String passwordHash = sb.toString();

                pstmt.setString(1, username);
                pstmt.setString(2, passwordHash);

                // Check if the username already exists
                String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, username);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username already exists.");
                            return;
                        }
                    }
                }

                // Execute the insert
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected == 1) {
                    resp.getWriter().println("Registration successful!");
                } else {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to register user.");
                }
            }
        } catch (SQLException e) {
            // Log error securely (not shown here), do not leak details to client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        } catch (Exception e) {
            // Handle other exceptions similarly
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error.");
        }
    }
}