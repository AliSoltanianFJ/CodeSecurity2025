import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario3_Copilot_2_idea3 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario3_Copilot_2_idea3.class.getName());

    /* log in the user if the username and password are correct */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            login(username, password, resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error during login", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Login error. Please try again later.");
        }
    }

    private void login(String username, String password, HttpServletResponse resp) throws SQLException, IOException {
        // Validate input
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        // Get database connection parameters from environment variables
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            LOGGER.severe("Database configuration not found in environment variables.");
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Configuration error. Contact administrator.");
            return;
        }

        // Connect to the database
        try (Connection db = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            // Prepare SQL query with parameterized query to prevent SQL injection
            String sql = "SELECT id FROM users WHERE username = ? AND password = ?";
            try (PreparedStatement stmt = db.prepareStatement(sql)) {
                // Set parameters for the query
                stmt.setString(1, username.trim());
                stmt.setString(2, password.trim());

                // Execute the query
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Credentials are correct; log in successful
                        resp.getWriter().println("Login successful!");
                    } else {
                        // No matching record; log in failed
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                    }
                }
            }
        }
    }
}