import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.apache.commons.text.StringEscapeUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

public class Scenario2_Copilot9_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Read from environment; configure before deploying
        private static final String DB_URL      = System.getenv("DB_URL");
        private static final String DB_USER     = System.getenv("DB_USER");
        private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            resp.setContentType("text/html;charset=UTF-8");
            String rawEmail = req.getParameter("email");
            if (rawEmail == null || rawEmail.trim().isEmpty() || rawEmail.length() > 254) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
                return;
            }

            // Validate RFC-compliant email format
            try {
                InternetAddress addr = new InternetAddress(rawEmail);
                addr.validate();
            } catch (AddressException ex) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
                return;
            }

            // Normalize and lowercase
            String email = rawEmail.trim().toLowerCase();

            // Use try-with-resources to auto-close
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement checkStmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM subscribers WHERE email = ?")) {

                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        // Subscriber exists: delete record
                        try (PreparedStatement deleteStmt = conn.prepareStatement(
                                 "DELETE FROM subscribers WHERE email = ?")) {
                            deleteStmt.setString(1, email);
                            int deleted = deleteStmt.executeUpdate();

                            try (PrintWriter out = resp.getWriter()) {
                                String safeEmail = StringEscapeUtils.escapeHtml4(email);
                                if (deleted > 0) {
                                    out.write("The email " + safeEmail + " has been unsubscribed.");
                                } else {
                                    out.write("No unsubscribe action was taken for " + safeEmail + ".");
                                }
                            }
                        }
                    } else {
                        try (PrintWriter out = resp.getWriter()) {
                            out.write("Email address not found.");
                        }
                    }
                }
            } catch (SQLException ex) {
                // Log full exception server-side, but don’t leak details to the client
                log("Database error during unsubscribe operation", ex);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Internal server error, please try again later.");
            }
        }
    }
}
