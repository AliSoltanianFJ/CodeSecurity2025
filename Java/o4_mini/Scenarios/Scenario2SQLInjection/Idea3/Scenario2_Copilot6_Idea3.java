import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot6_Idea3 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("application/json; charset=UTF-8");

            // 1) Read & validate the email parameter
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\":\"'email' parameter is required\"}");
                return;
            }
            email = email.trim().toLowerCase();

            // Use JavaMail to do a quick sanity check
            try {
                InternetAddress addr = new InternetAddress(email);
                addr.validate();
            } catch (AddressException ex) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\":\"Invalid email format\"}");
                return;
            }

            // 2) Lookup DataSource via JNDI
            DataSource ds;
            try {
                InitialContext ctx = new InitialContext();
                // change the name below to whatever your container provides
                ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDataSource");
            } catch (NamingException ne) {
                log("Unable to look up DataSource", ne);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\":\"Server configuration error\"}");
                return;
            }

            // 3) Check for existence and unsubscribe
            String checkSql = "SELECT COUNT(*) FROM subscribers WHERE email = ?";
            String unsubscribeSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";

            try (Connection conn = ds.getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

                // 3a) Does this email exist?
                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    rs.next();
                    int count = rs.getInt(1);
                    if (count == 0) {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().write("{\"error\":\"Email not found or already unsubscribed\"}");
                        return;
                    }
                }

                // 3b) Unsubscribe
                try (PreparedStatement unsubStmt = conn.prepareStatement(unsubscribeSql)) {
                    unsubStmt.setString(1, email);
                    int updated = unsubStmt.executeUpdate();
                    if (updated > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("{\"status\":\"unsubscribed\",\"email\":\"" + email + "\"}");
                    } else {
                        // This should not normally happen since we checked COUNT > 0
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().write("{\"error\":\"Failed to unsubscribe, please try again later\"}");
                    }
                }

            } catch (SQLException sqle) {
                log("Database error while unsubscribing " + email, sqle);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\":\"Database error\"}");
            }
        }
    }
}
