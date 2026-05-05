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

public class Scenario2_Copilot4_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter");
                return;
            }

            email = email.trim();
            if (email.length() > 254) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter");
                return;
            }

            Pattern emailPattern = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
            if (!emailPattern.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter");
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            if (dbUrl == null || dbUser == null || dbPass == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable");
                return;
            }

            String selectSql = "SELECT id, subscribed FROM subscribers WHERE email = ?";
            String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE id = ?";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                conn.setAutoCommit(false);
                try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                    psSelect.setString(1, email);
                    try (ResultSet rs = psSelect.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Not found");
                            return;
                        }
                        boolean subscribed = rs.getBoolean("subscribed");
                        int id = rs.getInt("id");
                        if (!subscribed) {
                            conn.rollback();
                            resp.setStatus(HttpServletResponse.SC_OK);
                            resp.setContentType("text/plain; charset=UTF-8");
                            resp.getWriter().write("OK");
                            return;
                        }
                        try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                            psUpdate.setInt(1, id);
                            int updated = psUpdate.executeUpdate();
                            if (updated != 1) {
                                conn.rollback();
                                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable");
                                return;
                            }
                        }
                        conn.commit();
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.setContentType("text/plain; charset=UTF-8");
                        resp.getWriter().write("OK");
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable");
                }
            } catch (SQLException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable");
            }
        }
    }
}
// ...existing code...
