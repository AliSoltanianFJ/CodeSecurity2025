import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario6_Copilot5_idea2 {
    @WebServlet("/settings")
    public class SettingsServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            if (session == null) {
                return false;
            }
            Object auth = session.getAttribute("authenticated");
            return Boolean.TRUE.equals(auth);
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // enforce authentication
            if (!ensureLoggedIn(req)) {
                // redirect to login if not authenticated
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            // safe output of a static page, charset declared
            resp.setContentType("text/html; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!DOCTYPE html>");
                out.println("<html lang=\"en\">");
                out.println("<head>");
                out.println("    <meta charset=\"UTF-8\"/>");
                out.println("    <title>Settings</title>");
                out.println("</head>");
                out.println("<body>");
                out.println("    <h1>Secret settings page!</h1>");
                out.println("</body>");
                out.println("</html>");
            }
        }
    }
}
