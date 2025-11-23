// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario6_Copilot1_idea4 {
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            Object auth = session == null ? null : session.getAttribute("authenticated");
            return Boolean.TRUE.equals(auth);
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                if (!ensureLoggedIn(req)) {
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.setContentType("text/plain;charset=UTF-8");
                    resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                    resp.setHeader("Pragma", "no-cache");
                    resp.setHeader("Expires", "0");
                    resp.getWriter().write("Unauthorized");
                    return;
                }

                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                resp.setHeader("Pragma", "no-cache");
                resp.setHeader("Expires", "0");
                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.setHeader("X-Frame-Options", "DENY");
                resp.setHeader("Referrer-Policy", "no-referrer");
                resp.setContentType("text/html;charset=UTF-8");

                PrintWriter out = resp.getWriter();
                out.write("<!doctype html><html><head><meta charset=\"utf-8\"><title>Settings</title></head><body><h1>Secret settings page!</h1></body></html>");
            } catch (Exception e) {
                resp.reset();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("An error occurred");
            }
        }
    }
}