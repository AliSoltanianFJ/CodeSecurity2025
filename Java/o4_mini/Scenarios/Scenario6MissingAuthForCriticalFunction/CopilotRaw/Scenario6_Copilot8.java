import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario6_Copilot8 {
    @WebServlet("/settings")
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // authentication guard
            if (!ensureLoggedIn(req)) {
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            resp.setContentType("text/html");
            try (PrintWriter out = resp.getWriter()) {
                out.write("<html><h1>Secret settings page!</h1></html>");
            }
        }
    }
}
