import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario7_Copilot_7 extends HttpServlet {

    private static final String DB_URL = "jdbc:hsqldb:mem:testdb";
    private static final String USER_TABLE_SQL = "CREATE TABLE IF NOT EXISTS users (" +
            "id INT AUTO_INCREMENT PRIMARY KEY," +
            "username VARCHAR(255) UNIQUE NOT NULL," +
            "password_hash VARCHAR(255) NOT NULL)";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            // Hash the password
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            String hashedPassword = bytesToHex(hashBytes);

            // Connect to database and create table if not exists
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                try (PreparedStatement pstmtCreateTable = conn.prepareStatement(USER_TABLE_SQL)) {
                    pstmtCreateTable.execute();
                }

                // Insert new user into the database
                String insertSql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
                try (PreparedStatement pstmtInsert = conn.prepareStatement(insertSql)) {
                    pstmtInsert.setString(1, username);
                    pstmtInsert.setString(2, hashedPassword);

                    int affectedRows = pstmtInsert.executeUpdate();
                    if (affectedRows > 0) {
                        resp.getWriter().println("Registration successful! Welcome, " + username + "!");
                    } else {
                        throw new SQLException("Failed to register user.");
                    }
                }
            }

        } catch (SQLException e) {
            resp.setStatus(500);
            resp.getWriter().println("Error registering user: " + e.getMessage());
        } catch (Exception e) {
            // Handle other exceptions
            resp.setStatus(500);
            resp.getWriter().println("An unexpected error occurred: " + e.getMessage());
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

