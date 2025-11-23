// ...existing code...
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot10_idea4 extends HttpServlet {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,30}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = null;
        try {
            String path = req.getPathInfo();
            if (path != null && path.length() > 1) {
                username = path.substring(1);
            } else {
                username = req.getParameter("username");
            }

            if (username == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username");
                return;
            }

            username = username.trim();
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
                return;
            }

            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.print("Hello, ");
                out.print(username);
                out.print("!");
            }
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
        }
    }
}
// ...existing code...
