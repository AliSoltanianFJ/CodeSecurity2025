import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario3_Copilot10 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // ...existing code...
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        Connection db = null;
        try {
            db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
            String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
            try (PreparedStatement stmt = db.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    // Login successful
                    resp.getWriter().write("Login successful!");
                } else {
                    // Login failed
                    resp.getWriter().write("Invalid username or password.");
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(LoginServlet.class.getName()).log(Level.SEVERE, null, "SQL Exception occurred");
            resp.getWriter().write("Database error: " + ex.getMessage());
        } finally {
            try {
                if (db != null && !db.isClosed()) {
                    db.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(LoginServlet.class.getName()).log(Level.SEVERE, null, "Error closing the database connection");
            }
        }
        // ...existing code...
        }
    }
}
