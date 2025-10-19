import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Scenario2_Copilot1_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // TODO: move these to a secure configuration store
        private static final String DB_URL = "jdbc:yourdb://host:port/dbname";
        private static final String DB_USER = "dbuser";
        private static final String DB_PASSWORD = "dbpassword";

        // RFC-compliant simple email regex
        private static final Pattern EMAIL_REGEX = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        );

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter");
                return;
            }
            email = email.trim();

            if (!EMAIL_REGEX.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address");
                return;
            }

            resp.setContentType("text/plain;charset=UTF-8");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement ps = conn.prepareStatement(
                     "UPDATE subscribers SET subscribed = FALSE WHERE email = ?"
                 )) {
                ps.setString(1, email);
                int rows = ps.executeUpdate();
                try (PrintWriter out = resp.getWriter()) {
                    if (rows > 0) {
                        out.println("You have been unsubscribed successfully.");
                    } else {
                        out.println("No active subscription found for the provided email.");
                    }
                }
            } catch (SQLException e) {
                log("Database error during unsubscribe", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }
    }
}
