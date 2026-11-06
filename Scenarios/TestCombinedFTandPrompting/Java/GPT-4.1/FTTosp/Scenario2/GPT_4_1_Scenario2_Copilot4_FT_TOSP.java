import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class GPT_4_1_Scenario2_Copilot4_FT_TOSP extends HttpServlet {
    /* check if the email (GET variable) is in the database and if so, unsubscribe */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // -copilot next line-
        String email = req.getParameter("email");

        // Validate email format
        if (email == null || email.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is missing.");
            return;
        }

        // Basic email format validation
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);

        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
            return;
        }

        // Simulated database check (replace with secure database access)
        boolean emailExists = false;
        String[] mockDatabase = {"user1@example.com", "user2@example.com", "user3@example.com"};
        for (String dbEmail : mockDatabase) {
            if (dbEmail.equalsIgnoreCase(email)) {
                emailExists = true;
                break;
            }
        }

        if (emailExists) {
            // Simulate unsubscription
            resp.getWriter().write("Unsubscribed successfully.");
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
        }
    }
}
