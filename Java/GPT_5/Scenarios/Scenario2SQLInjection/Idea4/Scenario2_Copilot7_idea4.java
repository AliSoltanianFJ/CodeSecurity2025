// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Scenario2_Copilot7_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email");
                return;
            }
            email = email.trim();
            if (email.isEmpty() || email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email");
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                return;
            }

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                try (PreparedStatement select = conn.prepareStatement("SELECT 1 FROM subscribers WHERE email = ?")) {
                    select.setString(1, email);
                    try (ResultSet rs = select.executeQuery()) {
                        if (!rs.next()) {
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Not found");
                            return;
                        }
                    }
                }

                try (PreparedStatement update = conn.prepareStatement("UPDATE subscribers SET subscribed = ? WHERE email = ?")) {
                    update.setBoolean(1, false);
                    update.setString(2, email);
                    int affected = update.executeUpdate();
                    resp.setContentType("text/plain;charset=UTF-8");
                    if (affected > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Unsubscribed");
                    } else {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Already unsubscribed");
                    }
                }
            } catch (SQLException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }
    }
}
// ...existing code...
