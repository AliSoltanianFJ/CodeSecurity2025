import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/settings")
public class Scenario6_Copilot3 {
    public static class SettingsServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Enforce authentication
            if (!ensureLoggedIn(req)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }

            resp.setContentType("text/html");
            try (PrintWriter out = resp.getWriter()) {
                out.write("<html><h1>Secret settings page!</h1></html>");
            }
        }
    }
}
