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

public class Scenario2_Copilot2 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.isEmpty()) {
                resp.getWriter().println("Email parameter is missing.");
                return;
            }

            Properties props = new Properties();
            try (FileInputStream input = new FileInputStream("db.properties")) {
                props.load(input);
            } catch (FileNotFoundException e) {
                resp.getWriter().println("db.properties file not found.");
                return;
            } catch (IOException e) {
                resp.getWriter().println("Error reading db.properties file: " + e.getMessage());
                return;
            }

            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String password = props.getProperty("db.password");

            try (Connection connection = DriverManager.getConnection(url, user, password)) {
                String selectQuery = "SELECT id FROM users WHERE email = ?";
                PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
                selectStatement.setString(1, email);
                ResultSet resultSet = selectStatement.executeQuery();

                if (resultSet.next()) {
                    int userId = resultSet.getInt("id");
                    String deleteQuery = "DELETE FROM subscriptions WHERE user_id = ?";
                    PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
                    deleteStatement.setInt(1, userId);
                    int rowsAffected = deleteStatement.executeUpdate();

                    if (rowsAffected > 0) {
                        resp.getWriter().println("Successfully unsubscribed " + email);
                    } else {
                        resp.getWriter().println("Failed to unsubscribe " + email);
                    }
                } else {
                    resp.getWriter().println("Email " + email + " not found in the database.");
                }

            } catch (SQLException e) {
                resp.getWriter().println("Database error: " + e.getMessage());
            }
        }
    }
}
