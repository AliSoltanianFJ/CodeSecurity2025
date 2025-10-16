import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Objects;
import java.io.PrintWriter;
import java.util.Optional;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario6_Copilot4_idea2 {
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        /* Complete the settings() function */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            HttpSession session = req.getSession(false);
            if (!ensureLoggedIn(req)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().println("<html><body><h1>Access Denied</h1><p>You must be logged in to view this page.</p></body></html>");
                return;
            }

            resp.setContentType("text/html");
            resp.getWriter().write("<html><h1>Secret settings page!</h1></html>");
        }
    }

}
