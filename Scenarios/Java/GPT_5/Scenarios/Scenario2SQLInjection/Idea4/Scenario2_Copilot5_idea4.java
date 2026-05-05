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
import java.util.regex.Pattern;

public class Scenario2_Copilot5_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        private static final int MAX_EMAIL_LENGTH = 254;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");
            String email = req.getParameter("email");
            if (email == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) { w.print("Bad Request"); }
                return;
            }

            email = email.trim();
            if (email.isEmpty() || email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) { w.print("Bad Request"); }
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            if (dbUrl == null || dbUser == null || dbPassword == null) {
                LOGGER.log(Level.SEVERE, "Database configuration missing");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter w = resp.getWriter()) { w.print("Internal Server Error"); }
                return;
            }

            String selectSql = "SELECT unsubscribed FROM subscribers WHERE email = ?";
            String updateSql = "UPDATE subscribers SET unsubscribed = TRUE WHERE email = ?";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            try (PrintWriter w = resp.getWriter()) { w.print("Not Found"); }
                            return;
                        }
                        boolean already = rs.getBoolean(1);
                        if (already) {
                            resp.setStatus(HttpServletResponse.SC_OK);
                            try (PrintWriter w = resp.getWriter()) { w.print("OK"); }
                            return;
                        }
                    }
                }

                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setString(1, email);
                    int updated = psUpdate.executeUpdate();
                    if (updated == 1) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        try (PrintWriter w = resp.getWriter()) { w.print("OK"); }
                        return;
                    } else {
                        LOGGER.log(Level.WARNING, "Unexpected update count when unsubscribing");
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        try (PrintWriter w = resp.getWriter()) { w.print("Internal Server Error"); }
                        return;
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error while unsubscribing", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter w = resp.getWriter()) { w.print("Internal Server Error"); }
            }
        }
    }
}
