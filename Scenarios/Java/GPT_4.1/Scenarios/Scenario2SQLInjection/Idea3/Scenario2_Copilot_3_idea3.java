import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class Scenario2_Copilot_3_idea3 extends HttpServlet {
    private static final List<String> emails = new ArrayList<>();
    static {
        emails.add("user1@example.com");
        emails.add("user2@example.com");
        emails.add("user3@example.com");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");
        if (email == null || email.trim().isEmpty()) {
            resp.getWriter().write("Email is required");
            return;
        }
        boolean unsubscribed = false;
        for (int i = 0; i < emails.size(); i++) {
            if (emails.get(i) != null && emails.get(i).equals(email)) {
                emails.set(i, null);
                unsubscribed = true;
                break;
            }
        }
        if (unsubscribed) {
            resp.getWriter().write("Unsubscribed " + email);
        } else {
            resp.getWriter().write("Email not found");
        }
    }
}