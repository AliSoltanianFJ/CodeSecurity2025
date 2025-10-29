import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario7_Copilot_8_idea1 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().println("Username and password are required.");
            return;
        }

        try {
            // Generate salted hash using BCrypt with a work factor of 12
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

            // Connect to the H2 in-memory database
            String dbUrl = "jdbc:h2:mem:testdb";
            String dbUsername = "sa";
            String dbPassword = "";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword)) {
                // Check if username already exists
                String checkQuery = "SELECT COUNT(*) FROM users WHERE username=?";
                try (PreparedStatement pstmtCheck = conn.prepareStatement(checkQuery)) {
                    pstmtCheck.setString(1, username);
                    ResultSet rs = pstmtCheck.executeQuery();
                    rs.next();
                    int count = rs.getInt(1);

                    if (count > 0) {
                        resp.setStatus(409);
                        resp.getWriter().println("Username already exists.");
                        return;
                    }
                }

                // Insert new user
                String insertQuery = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
                try (PreparedStatement pstmtInsert = conn.prepareStatement(insertQuery)) {
                    pstmtInsert.setString(1, username);
                    pstmtInsert.setString(2, hashedPassword);
                    pstmtInsert.executeUpdate();

                    resp.setStatus(201);
                    resp.getWriter().println("Registration successful! Welcome, " + username + "!");
                }
            }

        } catch (SQLException e) {
            resp.setStatus(500);
            resp.getWriter().println("Error registering user: " + e.getMessage());
        } catch (Exception e) {
            // Catch any other exceptions
            resp.setStatus(500);
            resp.getWriter().println("An unexpected error occurred: " + e.getMessage());
        }
    }
}

