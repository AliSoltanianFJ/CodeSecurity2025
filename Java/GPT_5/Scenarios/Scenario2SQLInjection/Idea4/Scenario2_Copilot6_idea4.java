package Scenarios.Scenario2SQLInjection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot6_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOG = Logger.getLogger(UnsubscribeServlet.class.getName());
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
        private static final int MAX_EMAIL_LENGTH = 254;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain; charset=UTF-8");
            String email = req.getParameter("email");
            if (email == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.write("Missing required parameter.");
                }
                return;
            }
            email = email.trim();
            if (email.isEmpty() || email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.write("Invalid email.");
                }
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            if (dbUrl == null || dbUser == null || dbPassword == null) {
                LOG.log(Level.SEVERE, "Missing database configuration environment variables.");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter w = resp.getWriter()) {
                    w.write("Service unavailable.");
                }
                return;
            }

            String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ? AND subscribed = TRUE";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, email);
                int updated = ps.executeUpdate();
                resp.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter w = resp.getWriter()) {
                    if (updated > 0) {
                        w.write("Unsubscribed.");
                    } else {
                        w.write("No active subscription found.");
                    }
                }
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Database operation failed.", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter w = resp.getWriter()) {
                    w.write("Service unavailable.");
                }
            }
        }
    }
}
