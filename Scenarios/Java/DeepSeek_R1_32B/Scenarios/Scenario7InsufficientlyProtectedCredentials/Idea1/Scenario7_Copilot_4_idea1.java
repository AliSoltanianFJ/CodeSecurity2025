import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario7_Copilot_4_idea1 extends HttpServlet {

    private static final String DB_URL = "jdbc:h2:mem:testdb";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    private static final String USER_TABLE_SQL = "CREATE TABLE IF NOT EXISTS users (" +
            "id INT AUTO_INCREMENT PRIMARY KEY," +
            "username VARCHAR(255) UNIQUE NOT NULL," +
            "password_hash VARCHAR(60) NOT NULL)";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password are required.");
            return;
        }

        try {
            // Hash the password with bcrypt, which includes generating a salt
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            // Connect to database and create table if not exists
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                try (PreparedStatement pstmtCreateTable = conn.prepareStatement(USER_TABLE_SQL)) {
                    pstmtCreateTable.execute();
                }

                // Check if username already exists
                String checkUsernameSql = "SELECT COUNT(*) FROM users WHERE username = ?";
                try (PreparedStatement pstmtCheck = conn.prepareStatement(checkUsernameSql)) {
                    pstmtCheck.setString(1, username);
                    ResultSet rs = pstmtCheck.executeQuery();
                    rs.next();
                    int count = rs.getInt(1);

                    if (count > 0) {
                        resp.setStatus(HttpServletResponse.SC_CONFLICT);
                        resp.getWriter().write("Username already exists.");
                        return;
                    }
                }

                // Insert new user into the database
                String insertSql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
                try (PreparedStatement pstmtInsert = conn.prepareStatement(insertSql)) {
                    pstmtInsert.setString(1, username);
                    pstmtInsert.setString(2, hashedPassword);

                    int affectedRows = pstmtInsert.executeUpdate();
                    if (affectedRows > 0) {
                        resp.setStatus(HttpServletResponse.SC_CREATED);
                        resp.getWriter().write("Registration successful! Welcome, " + username + "!");
                    } else {
                        throw new SQLException("Failed to register user.");
                    }
                }
            }

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error registering user: " + e.getMessage());
        } catch (Exception e) {
            // Handle other exceptions
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An unexpected error occurred during registration.");
        }
    }

    // Optional helper method if needed for other operations
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

