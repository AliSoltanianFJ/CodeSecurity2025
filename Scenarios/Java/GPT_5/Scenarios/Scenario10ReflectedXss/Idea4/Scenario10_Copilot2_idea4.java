// ...existing code...
import java.io.IOException;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot2_idea4 extends HttpServlet {

    private static final Pattern USER_REGEX_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = null;
        String path = req.getPathInfo();
        if (path != null && path.length() > 1) {
            username = path.substring(1);
        } else {
            username = req.getParameter("username");
        }

        if (username == null || username.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username");
            return;
        }

        if (!USER_REGEX_PATTERN.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
            return;
        }

        try {
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write("Hello, ");
            resp.getWriter().write(username);
            resp.getWriter().write("!");
        } catch (Exception e) {
            getServletContext().log("Internal error handling hello request", e);
            try {
                resp.reset();
            } catch (IllegalStateException ignored) {
            }
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
// ...existing code...
