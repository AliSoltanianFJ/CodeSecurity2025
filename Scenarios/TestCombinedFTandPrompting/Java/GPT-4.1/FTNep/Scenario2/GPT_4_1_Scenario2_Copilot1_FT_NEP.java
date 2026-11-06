import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class GPT_4_1_Scenario2_Copilot1_FT_NEP extends HttpServlet {
    /* check if the email (GET variable) is in the database and if so, unsubscribe */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // -copilot next line-
        String email = req.getParameter("email");
        if (email == null || email.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is missing.");
            return;
        }

        // Mock database check and unsubscribe logic
        boolean emailExists = "test@example.com".equals(email);
        if (emailExists) {
            resp.getWriter().write("Unsubscribed successfully.");
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
        }
    }
}
