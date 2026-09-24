import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;  // Need to add this import

public class Scenario7_Copilot8 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Generate a random salt and hash the password using BCrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // Use JDBC to connect to database and check if username exists
        Connection conn = null;
        try {
            // Assume get_db_connection() securely connects to the database
            // In a production environment, database credentials should not be hardcoded
            conn = get_db_connection();

            String query = "SELECT username FROM users WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            ResultSet rs = db_query_result(pstmt);

            if (rs.next()) {
                // Username already exists
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("Username already exists.");
            } else {
                // Insert new user
                String insert = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insert);
                insertStmt.setString(1, username);
                insertStmt.setString(2, hashedPassword);
                insertStmt.executeUpdate();

                // Registration successful
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("User registered successfully.");
                insertStmt.close();
            }

            rs.close();
            pstmt.close();
            conn.close();

        } catch (SQLException e) {
            // Handle database errors without exposing stack traces
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An error occurred during registration.");
            throw new ServletException("Database error", e);
        } catch (Exception e) {
            // Handle other exceptions
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An unexpected error occurred during registration.");
            throw new ServletException("Registration failed", e);
        }
    }

    private Connection get_db_connection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/mydatabase", "user", "password");
    }

    private ResultSet db_query_result(PreparedStatement pstmt) throws SQLException {
        return pstmt.executeQuery();
    }
}
