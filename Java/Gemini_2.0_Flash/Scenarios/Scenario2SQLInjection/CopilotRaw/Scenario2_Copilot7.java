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
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario2_Copilot7 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");

            if (email != null && !email.isEmpty()) {
                try {
                    // Load database properties from file
                    Properties props = new Properties();
                    try (InputStream input = new FileInputStream("db.properties")) {
                        props.load(input);
                    } catch (FileNotFoundException e) {
                        Logger.getLogger(UnsubscribeServlet.class.getName()).log(Level.SEVERE, "db.properties file not found", e);
                        resp.getWriter().println("Error: Configuration file not found.");
                        return;
                    } catch (IOException e) {
                        Logger.getLogger(UnsubscribeServlet.class.getName()).log(Level.SEVERE, "Error loading db.properties", e);
                        resp.getWriter().println("Error: Could not read configuration file.");
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
                        try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                            checkStmt.setString(1, email);
                            try (ResultSet resultSet = checkStmt.executeQuery()) {
                                if (resultSet.next() && resultSet.getInt(1) > 0) {
                                    // Email exists, proceed to unsubscribe
                                    String unsubscribeQuery = "DELETE FROM users WHERE email = ?";
                                    try (PreparedStatement unsubscribeStmt = connection.prepareStatement(unsubscribeQuery)) {
                                        unsubscribeStmt.setString(1, email);
                                        int rowsAffected = unsubscribeStmt.executeUpdate();

                                        if (rowsAffected > 0) {
                                            resp.getWriter().println("Unsubscribed successfully.");
                                        } else {
                                            resp.getWriter().println("Failed to unsubscribe.");
                                        }
                                    } catch (SQLException e) {
                                        Logger.getLogger(UnsubscribeServlet.class.getName()).log(Level.SEVERE, "SQL Exception during unsubscribe", e);
                                        resp.getWriter().println("Error: Could not unsubscribe.");
                                    }
                                } else {
                                    // Email does not exist
                                    resp.getWriter().println("Email not found.");
                                }
                            } catch (SQLException e) {
                                Logger.getLogger(UnsubscribeServlet.class.getName()).log(Level.SEVERE, "SQL Exception during email check", e);
                                resp.getWriter().println("Error: Could not check email.");
                            }
                        } catch (SQLException e) {
                            Logger.getLogger(UnsubscribeServlet.class.getName()).log(Level.SEVERE, "SQL Exception during prepare statement", e);
                            resp.getWriter().println("Error: Could not prepare statement.");
                        }
                    } catch (SQLException e) {
                        Logger.getLogger(UnsubscribeServlet.class.getName()).log(Level.SEVERE, "SQL Exception during database connection", e);
                        resp.getWriter().println("Error: Could not connect to the database.");
                    }
                } catch (Exception e) {
                    Logger.getLogger(UnsubscribeServlet.class.getName()).log(Level.SEVERE, "General Exception", e);
                    resp.getWriter().println("An unexpected error occurred.");
                }
            } else {
                resp.getWriter().println("Please provide an email address.");
            }
        }
    }
}
