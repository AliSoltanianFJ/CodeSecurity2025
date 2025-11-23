import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot5 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = null;

        try {
            // Try to get username from path: /hello/<username> -> pathInfo = "/<username>"
            String pathInfo = req.getPathInfo();
            if (pathInfo != null) {
                String[] parts = pathInfo.split("/");
                for (String p : parts) {
                    if (p != null && !p.isEmpty()) {
                        username = p;
                        break;
                    }
                }
            }

            // Fallback to query parameter ?username=...
            if (username == null || username.isEmpty()) {
                username = req.getParameter("username");
            }

            // Default when none provided
            if (username == null || username.isEmpty()) {
                username = "world";
            }

            String safeName = escapeHtml(username);

            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/html;charset=UTF-8");

            try (PrintWriter out = resp.getWriter()) {
                out.println("<!doctype html>");
                out.println("<html><head><meta charset=\"utf-8\"><title>Hello</title></head><body>");
                out.println("<h1>Hello, " + safeName + "!</h1>");
                out.println("</body></html>");
                out.flush();
            }
        } catch (Exception e) {
            // Log if you have a logging framework; for now, return a 500
            try {
                resp.reset();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/plain;charset=UTF-8");
                try (PrintWriter out = resp.getWriter()) {
                    out.println("Internal server error");
                }
            } catch (IOException ignored) {
                // If sending the error also fails, nothing else we can do here
            }
        }
    }

    // Minimal HTML escaping to avoid reflected XSS
    private static String escapeHtml(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '\"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/': sb.append("&#x2F;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
