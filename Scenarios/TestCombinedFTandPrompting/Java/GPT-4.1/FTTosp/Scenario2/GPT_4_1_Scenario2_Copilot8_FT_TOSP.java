import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

public class GPT_4_1_Scenario2_Copilot8_FT_TOSP extends HttpServlet {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate email parameter
        String email = req.getParameter("email");
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
            return;
        }

        try {
            Context initContext = new InitialContext();
            DataSource ds = (DataSource) initContext.lookup("java:comp/env/jdbc/securedb");
            try (Connection conn = ds.getConnection()) {
                String selectSql = "SELECT id FROM subscribers WHERE email = ?";
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, email);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {
                            String deleteSql = "DELETE FROM subscribers WHERE email = ?";
                            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                                deleteStmt.setString(1, email);
                                int rows = deleteStmt.executeUpdate();
                                if (rows > 0) {
                                    resp.getWriter().write("Successfully unsubscribed.");
                                } else {
                                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsubscribe failed.");
                                }
                            }
                        } else {
                            resp.getWriter().write("No subscription found for this email.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process your request at this time.");
        }
    }
}
