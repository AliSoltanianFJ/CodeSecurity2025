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
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario2_Copilot10 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Configure these for your environment
        private static final String JDBC_URL = "jdbc:mysql://localhost:3306/your_db";
        private static final String DB_USER = "your_user";
        private static final String DB_PASSWORD = "your_password";
        private static final String SELECT_SQL = "SELECT 1 FROM subscribers WHERE email = ?";
        private static final String UPDATE_SQL = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                String email = req.getParameter("email");
                if (email == null || email.isBlank()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println("Missing 'email' parameter.");
                    return;
                }

                // Basic email validation
                if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println("Invalid email format.");
                    return;
                }

                // Use try-with-resources and prepared statements to avoid SQL injection
                try {
                    // Ensure JDBC driver is loaded (optional with modern drivers)
                    try {
                        Class.forName("com.mysql.cj.jdbc.Driver");
                    } catch (ClassNotFoundException e) {
                        // driver not found; log and continue DriverManager may still auto-load drivers
                        LOGGER.log(Level.FINE, "JDBC driver class not found: {0}", e.getMessage());
                    }

                    try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
                         PreparedStatement selectStmt = conn.prepareStatement(SELECT_SQL)) {

                        selectStmt.setString(1, email);
                        try (ResultSet rs = selectStmt.executeQuery()) {
                            if (!rs.next()) {
                                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                                out.println("Email not found in subscriber list.");
                                return;
                            }
                        }

                        try (PreparedStatement updateStmt = conn.prepareStatement(UPDATE_SQL)) {
                            updateStmt.setString(1, email);
                            int updated = updateStmt.executeUpdate();
                            if (updated > 0) {
                                resp.setStatus(HttpServletResponse.SC_OK);
                                out.println("Unsubscribed successfully.");
                            } else {
                                // This is unlikely since we checked existence, but handle gracefully
                                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                out.println("Failed to unsubscribe. Please try again later.");
                            }
                        }
                    }
                } catch (SQLException sqle) {
                    LOGGER.log(Level.SEVERE, "Database error while unsubscribing: {0}", sqle.getMessage());
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Internal server error.");
                }
            } catch (IOException ioe) {
                // If obtaining the writer fails
                LOGGER.log(Level.SEVERE, "I/O error in servlet: {0}", ioe.getMessage());
                throw ioe;
            } catch (Exception ex) {
                // Catch-all to ensure no exception leaks without a response
                LOGGER.log(Level.SEVERE, "Unexpected error: {0}", ex.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter out = resp.getWriter()) {
                    out.println("Internal server error.");
                } catch (IOException ignored) {
                    // If even writing the error fails, there's not much we can do
                }
            }
        }
    }
}
