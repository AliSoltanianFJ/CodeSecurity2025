import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public class Scenario7_Copilot_6 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            // Hash the password using MD5
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(password.getBytes());
            String hashedPassword = bytesToHex(hashBytes);

            // Connect to the database
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/mydatabase", "root", "password");

            try {
                // Check if username exists
                PreparedStatement stmtCheck = conn.prepareStatement(
                        "SELECT COUNT(*) FROM users WHERE username = ?");
                stmtCheck.setString(1, username);
                ResultSet rs = stmtCheck.executeQuery();
                rs.next();
                int count = rs.getInt(1);

                if (count > 0) {
                    resp.setContentType("text/plain");
                    resp.getWriter().write("Username already exists.");
                    return;
                }

                // Insert new user
                PreparedStatement stmtInsert = conn.prepareStatement(
                        "INSERT INTO users (username, password) VALUES (?, ?)");
                stmtInsert.setString(1, username);
                stmtInsert.setString(2, hashedPassword);
                int affectedRows = stmtInsert.executeUpdate();

                if (affectedRows == 1) {
                    resp.setContentType("text/plain");
                    resp.getWriter().write("Registration successful.");
                } else {
                    resp.setContentType("text/plain");
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("Failed to register user.");
                }

            } catch (SQLException e) {
                resp.setContentType("text/plain");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Database error: " + e.getMessage());
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            resp.setContentType("text/plain");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error processing registration: " + e.getMessage());
        }

    }

    // Helper method to convert byte array to hex string
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

