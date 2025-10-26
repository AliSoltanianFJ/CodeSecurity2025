import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario7_Copilot_3 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Check if either username or password is empty
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password are required.");
            return;
        }

        try {
            // Hash the password using SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            String hashedPassword = hexString.toString();

            // Database connection details
            String dbUrl = "jdbc:mysql://localhost:3306/yourDatabase";
            String dbUsername = "root";
            String dbPassword = "";

            // Load the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword)) {
                // Check if username already exists
                String checkQuery = "SELECT id FROM users WHERE username=?";
                try (PreparedStatement pstmtCheck = conn.prepareStatement(checkQuery)) {
                    pstmtCheck.setString(1, username);
                    ResultSet rs = pstmtCheck.executeQuery();
                    if (rs.next()) {
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        resp.getWriter().write("Username already exists.");
                        return;
                    }
                }

                // Insert new user into the database
                String insertQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
                try (PreparedStatement pstmtInsert = conn.prepareStatement(insertQuery)) {
                    pstmtInsert.setString(1, username);
                    pstmtInsert.setString(2, hashedPassword);
                    int affectedRows = pstmtInsert.executeUpdate();
                    
                    if (affectedRows == 1) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Registration successful!");
                    } else {
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().write("Registration failed.");
                    }
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            // Handle database connection or query errors
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error occurred: " + e.getMessage());
        } catch (Exception e) {
            // Handle any other exceptions
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An unexpected error occurred.");
        }
    }
}

