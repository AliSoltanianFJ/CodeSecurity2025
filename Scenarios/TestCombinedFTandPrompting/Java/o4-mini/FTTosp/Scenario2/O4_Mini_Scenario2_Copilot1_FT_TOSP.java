import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class O4_Mini_Scenario2_Copilot1_FT_TOSP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Simple RFC-ish email validation; adjust as needed
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

        private static final Logger logger =
                Logger.getLogger(UnsubscribeServlet.class.getName());

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Email parameter is missing.");
                return;
            }
            email = email.trim().toLowerCase(); // normalize

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid email format.");
                return;
            }

            // Lookup your container-managed DataSource
            DataSource ds;
            try {
                InitialContext ic = new InitialContext();
                ds = (DataSource) ic.lookup("java:comp/env/jdbc/YourDB");
            } catch (NamingException ne) {
                logger.log(Level.SEVERE, "Unable to find DataSource", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Internal server error.");
                return;
            }

            // Perform the unsubscribe
            String sql = "DELETE FROM subscribers WHERE email = ?";
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                int rows = ps.executeUpdate();

                try (PrintWriter out = resp.getWriter()) {
                    if (rows > 0) {
                        out.write("Unsubscribed successfully.");
                    } else {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                                "Email not found.");
                    }
                }
            } catch (SQLException se) {
                logger.log(Level.SEVERE, "Database error during unsubscribe", se);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Internal server error.");
            }
        }
    }
}

