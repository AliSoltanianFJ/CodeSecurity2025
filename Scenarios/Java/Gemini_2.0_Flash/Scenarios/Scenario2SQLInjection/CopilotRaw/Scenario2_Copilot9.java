import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Scenario2_Copilot9 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");

            if (email != null && !email.isEmpty()) {
                try {
                    // Load database properties from file
                    Properties props = new Properties();
                    try (FileInputStream input = new FileInputStream("database.properties")) {
                        props.load(input);
                    } catch (FileNotFoundException e) {
                        resp.getWriter().println("Database configuration file not found.");
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        return;
                    } catch (IOException e) {
                        resp.getWriter().println("Error reading database configuration file.");
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        return;
                    }

                    // Database connection details
                    String dbUrl = props.getProperty("db.url");
                    String dbUser = props.getProperty("db.user");
                    String dbPass = props.getProperty("db.password");

                    // Connect to the database
                    try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {

                        // Check if the email exists
                        String checkQuery = "SELECT id FROM users WHERE email = ?";
                        try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                            checkStmt.setString(1, email);
                            try (ResultSet resultSet = checkStmt.executeQuery()) {
                                if (resultSet.next()) {
                                    // Email exists, proceed with unsubscription
                                    int userId = resultSet.getInt("id");

                                    // Unsubscribe the user (e.g., set a flag in the database)
                                    String unsubscribeQuery = "UPDATE users SET subscribed = false WHERE id = ?";
                                    try (PreparedStatement unsubscribeStmt = connection.prepareStatement(unsubscribeQuery)) {
                                        unsubscribeStmt.setInt(1, userId);
                                        int rowsUpdated = unsubscribeStmt.executeUpdate();

                                        if (rowsUpdated > 0) {
                                            resp.getWriter().println("Successfully unsubscribed " + email);
                                        } else {
                                            resp.getWriter().println("Failed to unsubscribe " + email);
                                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                        }
                                    }
                                } else {
                                    // Email does not exist
                                    resp.getWriter().println("Email " + email + " not found in the database.");
                                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                                }
                            }
                        }
                    } catch (SQLException e) {
                        resp.getWriter().println("Database error: " + e.getMessage());
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                } catch (Exception e) {
                    resp.getWriter().println("An unexpected error occurred: " + e.getMessage());
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } else {
                resp.getWriter().println("Email parameter is missing.");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }
}
