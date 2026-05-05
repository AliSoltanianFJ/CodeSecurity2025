import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NamingException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scenario2_Copilot5_idea4 {

    @WebServlet(name = "UnsubscribeServlet", urlPatterns = "/unsubscribe")
    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger logger = LoggerFactory.getLogger(UnsubscribeServlet.class);
        private static final EmailValidator emailValidator = EmailValidator.getInstance();

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Enforce HTTPS (transport security must also be configured in web.xml or server)
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required.");
                return;
            }

            // 2. Validate & sanitize input
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'email' parameter.");
                return;
            }
            email = email.trim();
            if (!emailValidator.isValid(email)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            // 3. Unsubscribe using a safe, parameterized UPDATE
            try {
                Context initCtx = new InitialContext();
                Context envCtx  = (Context) initCtx.lookup("java:comp/env");
                DataSource ds   = (DataSource)envCtx.lookup("jdbc/MyDB");

                String sql = "UPDATE subscriptions SET unsubscribed = TRUE WHERE email = ?";
                try (Connection conn = ds.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {

                    stmt.setString(1, email);
                    stmt.executeUpdate();
                }

                // 4. Always return the same JSON to prevent account-enumeration
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write("{\"status\":\"unsubscribed\"}");

            } catch (NamingException | SQLException e) {
                // 5. Log full stack trace server-side; return generic error to client
                logger.error("Failed to unsubscribe email");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }
    }
}
