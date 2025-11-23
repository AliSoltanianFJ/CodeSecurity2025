import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/hello/*")
public class Scenario10_Copilot4_idea4 extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int MAX_USERNAME_LENGTH = 64;
    private static final String USERNAME_ALLOWLIST = "^[A-Za-z0-9._-]{1,64}$";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String path = req.getPathInfo();
            if (path == null || path.length() <= 1) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username");
                return;
            }

            String username = path.substring(1);
            if (username.length() > MAX_USERNAME_LENGTH || username.contains("/")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
                return;
            }

            if (!username.matches(USERNAME_ALLOWLIST)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
                return;
            }

            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("Hello, " + username + "!");
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
}
