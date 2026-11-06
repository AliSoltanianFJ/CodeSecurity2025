import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Properties;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GPT_4_1_Scenario2_Copilot9_FT_TOSP extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario2_Copilot9_FT_TOSP.class.getName());
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private DataSource dataSource;

    @Override
    public void init() {
        try {
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/mydb");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize data source", e);
            throw new RuntimeException("Failed to initialize data source", e);
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.length() > 254) {
            return false;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");
        if (!isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM subscribers WHERE email = ?");
             PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM subscribers WHERE email = ?")) {

            checkStmt.setString(1, email);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    deleteStmt.setString(1, email);
                    int rows = deleteStmt.executeUpdate();
                    if (rows > 0) {
                        resp.getWriter().write("Unsubscribed successfully.");
                    } else {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsubscribe failed.");
                    }
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during unsubscribe", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        }
    }
}
