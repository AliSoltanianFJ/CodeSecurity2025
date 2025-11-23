// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot8 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");
            PrintWriter out = resp.getWriter();

            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Missing or empty 'email' parameter.");
                return;
            }

            // Configure these for your environment
            final String DB_URL = "jdbc:postgresql://localhost:5432/yourdb";
            final String DB_USER = "youruser";
            final String DB_PASS = "yourpassword";

            // Use prepared statements to avoid SQL injection
            try {
                // If your JDBC driver requires explicit loading, uncomment the next line and adjust driver class.
                // Class.forName("org.postgresql.Driver");

                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                    // Check existence
                    final String SELECT_SQL = "SELECT id FROM subscribers WHERE email = ? LIMIT 1";
                    try (PreparedStatement ps = conn.prepareStatement(SELECT_SQL)) {
                        ps.setString(1, email);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                                out.println("Email not found.");
                                return;
                            }
                        }
                    }

                    // Unsubscribe (example: set subscribed flag to false). Adjust column/table names as needed.
                    final String UPDATE_SQL = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
                    try (PreparedStatement ps2 = conn.prepareStatement(UPDATE_SQL)) {
                        ps2.setString(1, email);
                        int rows = ps2.executeUpdate();
                        if (rows > 0) {
                            resp.setStatus(HttpServletResponse.SC_OK);
                            out.println("Unsubscribed successfully.");
                        } else {
                            // This should be rare because we checked existence, but handle gracefully
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            out.println("Failed to unsubscribe. Please try again later.");
                        }
                    }
                }
            } catch (SQLException e) {
                // Log the error server-side as appropriate (not printed to the client in production)
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Database error.");
            } catch (Exception e) {
                // Catch-all for unexpected exceptions
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Server error.");
            }
        }
    }
}
// ...existing code...
