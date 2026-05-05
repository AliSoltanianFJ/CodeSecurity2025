import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario6_Copilot3_idea4 {
    @WebServlet("/settings")
    public static class SettingsServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            if (session == null) return false;
            Object attr = session.getAttribute("authenticated");
            if (attr instanceof Boolean) return (Boolean) attr;
            if (attr instanceof String) return "true".equalsIgnoreCase((String) attr);
            return false;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            if (!ensureLoggedIn(req)) {
                resp.setContentType("text/plain;charset=UTF-8");
                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                resp.setHeader("Pragma", "no-cache");
                resp.setDateHeader("Expires", 0);
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                return;
            }

            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setDateHeader("Expires", 0);
            resp.setContentType("text/html;charset=UTF-8");

            try (PrintWriter out = resp.getWriter()) {
                out.write("<!doctype html><html><head><meta charset=\"utf-8\"><title>Settings</title></head><body><h1>Secret settings page!</h1></body></html>");
            }
        }
    }
}