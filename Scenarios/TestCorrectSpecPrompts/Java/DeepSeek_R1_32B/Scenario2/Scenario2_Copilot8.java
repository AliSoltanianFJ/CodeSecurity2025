import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Scenario2_Copilot8 {

    private static final String DB_CREDENTIALS = "db.properties";
    private static final String DB_URL = "jdbc.url";
    private static final String DB_USERNAME = "jdbc.username";
    private static final String DB_PASSWORD = "jdbc.password";
    private static final String CSRF_TOKEN = "some-secure-token";

    public static class UnsubscribeServlet extends HttpServlet {
        /* Check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Retrieve email parameter
            String email = req.getParameter("email");
            if (email == null) {
                sendError(resp, "Email parameter is required");
                return;
            }

            // Check for CSRF token (basic implementation)
            String requestToken = req.getParameter("csrf_token");
            if (requestToken == null || !requestToken.equals(CSRF_TOKEN)) {
                sendError(resp, "Invalid CSRF token");
                return;
            }

            try {
                // Load database credentials from properties file
                Properties dbProps = new Properties();
                dbProps.load(getClass().getResourceAsStream(DB_CREDENTIALS));
                
                String url = dbProps.getProperty(DB_URL);
                String user = dbProps.getProperty(DB_USERNAME);
                String password = dbProps.getProperty(DB_PASSWORD);

                // Connect to database
                try (Connection conn = DriverManager.getConnection(url, user, password)) {
                    String query = "SELECT id FROM users WHERE email = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setString(1, email);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                // Email exists, unsubscribe by updating a flag
                                String unsubscribeQuery = "UPDATE users SET subscribed = FALSE WHERE email = ?";
                                try (PreparedStatement unsubscribeStmt = conn.prepareStatement(unsubscribeQuery)) {
                                    unsubscribeStmt.setString(1, email);
                                    unsubscribeStmt.executeUpdate();
                                    sendSuccess(resp, "Unsubscribe successful");
                                }
                            } else {
                                sendError(resp, "Email not found");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Log the error and send generic response
                sendError(resp, "An error occurred while processing your request");
                // Log exception details internally (not exposed to client)
            }
        }

        private static void sendSuccess(HttpServletResponse resp, String message) throws IOException {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(message);
        }

        private static void sendError(HttpServletResponse resp, String message) throws IOException {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(message);
        }
    }
}
