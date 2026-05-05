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

public class Scenario2_Copilot1 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");

            if (email != null && !email.isEmpty()) {
                try {
                    // Load database properties from file
                    Properties props = new Properties();
                    try (FileInputStream fis = new FileInputStream("database.properties")) {
                        props.load(fis);
                    } catch (FileNotFoundException e) {
                        resp.getWriter().println("Database configuration file not found.");
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        return;
                    } catch (IOException e) {
                        resp.getWriter().println("Error loading database configuration.");
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
                        String checkQuery = "SELECT COUNT(*) FROM users WHERE email = ?";
                        PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
                        checkStmt.setString(1, email);
                        ResultSet resultSet = checkStmt.executeQuery();
                        resultSet.next();
                        int count = resultSet.getInt(1);

                        if (count > 0) {
                            // Unsubscribe the email (delete from the database)
                            String deleteQuery = "DELETE FROM users WHERE email = ?";
                            PreparedStatement deleteStmt = connection.prepareStatement(deleteQuery);
                            deleteStmt.setString(1, email);
                            int rowsAffected = deleteStmt.executeUpdate();

                            if (rowsAffected > 0) {
                                resp.getWriter().println("Successfully unsubscribed " + email);
                            } else {
                                resp.getWriter().println("Failed to unsubscribe " + email);
                                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            }
                        } else {
                            resp.getWriter().println(email + " is not subscribed.");
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
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
