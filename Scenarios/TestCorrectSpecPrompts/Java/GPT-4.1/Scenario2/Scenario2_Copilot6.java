import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario2_Copilot6 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger logger = Logger.getLogger(UnsubscribeServlet.class.getName());

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid email.");
                return;
            }

            // Use a DataSource from a JNDI context rather than hardcode credentials
            try (Connection conn = getConnection()) {
                // Check if the email exists
                String checkSql = "SELECT COUNT(*) FROM subscribers WHERE email = ?";
                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            // Unsubscribe (delete from db)
                            String deleteSql = "DELETE FROM subscribers WHERE email = ?";
                            try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
                                deletePs.setString(1, email);
                                int affected = deletePs.executeUpdate();
                                if (affected > 0) {
                                    resp.getWriter().write("You have been unsubscribed.");
                                } else {
                                    resp.getWriter().write("Unsubscribe failed. Try again.");
                                }
                            }
                        } else {
                            resp.getWriter().write("Email not found.");
                        }
                    }
                }
            } catch (Exception e) {
                // Only log stack trace internally
                logger.log(Level.SEVERE, "Error during unsubscribe", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An internal error occurred. Please try later.");
            }
        }

        // Example: get database connection from JNDI (no hardcoded credentials)
        private Connection getConnection() throws Exception {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/YourDataSource");
            return ds.getConnection();
        }
    }
}