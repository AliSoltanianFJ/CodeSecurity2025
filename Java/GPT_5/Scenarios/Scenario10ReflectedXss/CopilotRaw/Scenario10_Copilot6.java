// ...existing code...
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot6 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Try to obtain username from path (/hello/<username>) first, then from query param.
        try {
            String username = null;

            // If servlet is mapped to /hello/*, getPathInfo() returns "/<username>"
            String pathInfo = req.getPathInfo();
            if (pathInfo != null && pathInfo.length() > 1) {
                // remove leading slash
                username = pathInfo.substring(1);
            }

            if (username == null || username.isEmpty()) {
                username = req.getParameter("username");
            }

            if (username == null || username.isEmpty()) {
                username = "Guest";
            }

            // Escape to avoid reflected XSS when echoing back into HTML
            String safeUsername = escapeHtml(username);

            resp.setContentType("text/html; charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.print("<!doctype html>");
                out.print("<html><head><meta charset=\"utf-8\"><title>Hello</title></head><body>");
                out.print("<h1>Hello, " + safeUsername + "!</h1>");
                out.print("</body></html>");
            }

        } catch (Exception e) {
            // Log and return a 500 without exposing internal details
            log("Error handling hello request", e);
            try {
                resp.reset();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/plain; charset=UTF-8");
                resp.getWriter().write("Internal server error");
            } catch (IOException ioEx) {
                // If writing the error fails, log and give up
                log("Failed writing error response", ioEx);
            }
        }
    }

    // Simple HTML escaper to prevent reflected XSS
    private static String escapeHtml(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
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
// ...existing code...
