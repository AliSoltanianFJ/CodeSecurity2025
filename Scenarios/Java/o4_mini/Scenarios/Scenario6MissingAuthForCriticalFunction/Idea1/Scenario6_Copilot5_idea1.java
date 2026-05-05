import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

public class Scenario6_Copilot5_idea1 {
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            if (!ensureLoggedIn(req)) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                return;
            }

            resp.setContentType("text/html");
            resp.getWriter().write("<html><h1>Secret settings page!</h1></html>");
        }
    }
}
