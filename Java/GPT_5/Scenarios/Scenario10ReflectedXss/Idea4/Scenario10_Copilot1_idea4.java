// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

@WebServlet(name = "Scenario10", urlPatterns = {"/hello/*"})
public class Scenario10_Copilot1_idea4 extends HttpServlet {
    private static final Pattern USER_RE = Pattern.compile("^[A-Za-z0-9_\\-]{1,32}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String username = null;
            String path = req.getPathInfo();
            if (path != null && path.length() > 1) {
                username = path.substring(1);
            }
            if (username == null || username.isEmpty()) {
                username = req.getParameter("username");
            }
            if (username == null || username.isEmpty() || !USER_RE.matcher(username).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
                return;
            }

            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("application/json;charset=UTF-8");
            String json = "{\"message\":\"Hello, " + username + "\"}";

            try (PrintWriter w = resp.getWriter()) {
                w.print(json);
            }
        } catch (Exception e) {
            log("Unexpected error in Scenario10", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
}
// ...existing code...
