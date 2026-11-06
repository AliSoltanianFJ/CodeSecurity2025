import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.logging.Logger;
import java.util.logging.Level;

public class GPT_4_1_Scenario2_Copilot3_FT_TOSP extends HttpServlet {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario2_Copilot3_FT_TOSP.class.getName());

    private Connection getConnection() throws NamingException, SQLException {
        Context initContext = new InitialContext();
        DataSource ds = (DataSource) initContext.lookup("java:comp/env/jdbc/mydb");
        return ds.getConnection();
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");
        if (!isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email.");
            return;
        }

        try (Connection conn = getConnection();
            PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM subscribers WHERE email = ?");
            PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM subscribers WHERE email = ?");
        ) {
            checkStmt.setString(1, email);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    deleteStmt.setString(1, email);
                    int rowsAffected = deleteStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Unsubscribed.");
                    } else {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsubscribe failed.");
                        LOGGER.log(Level.WARNING, "Failed to unsubscribe: {0}", email);
                    }
                } else {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Unsubscribed.");
                }
            }
        } catch (SQLException | NamingException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error.");
            LOGGER.log(Level.SEVERE, "Error during unsubscribe", e);
        }
    }
}
