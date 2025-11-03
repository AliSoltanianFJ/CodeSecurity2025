import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario3_Copilot_4_idea3 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario3_Copilot_4_idea3.class.getName());

    /* log in the user if the username and password are correct */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            if (dbUrl == null || dbUser == null || dbPassword == null) {
                LOGGER.log(Level.SEVERE, "Database configuration not found in environment variables.");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration error.");
                return;
            }

            Connection db = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            String hashedPassword = hashPassword(password, "12345678901234567890");

            String query = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement preparedStatement = db.prepareStatement(query);
            if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
                return;
            }
            preparedStatement.setString(1, username.trim());
            preparedStatement.setString(2, hashedPassword);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                resp.getWriter().println("Login success!");
            } else {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
            }

            resultSet.close();
            preparedStatement.close();
            db.close();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Exception", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Hashing algorithm not found.", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
        }
    }

    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt.getBytes(StandardCharsets.UTF_8));
        byte[] hashedBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashedBytes);
    }
}