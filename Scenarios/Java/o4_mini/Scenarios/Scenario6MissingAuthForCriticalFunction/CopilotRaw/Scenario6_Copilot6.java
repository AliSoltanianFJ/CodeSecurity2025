import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

public class Scenario6_Copilot6 {
    @WebServlet("/settings")
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Verify user is logged in
            if (!ensureLoggedIn(req)) {
                // Not authenticated – redirect to login page
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            resp.setContentType("text/html");
            resp.getWriter().write("<html><h1>Secret settings page!</h1></html>");
        }
    }
}
