// ...existing code...
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot7 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Try to obtain username from path "/hello/{username}" first, then fallback to ?username=
        String username = null;
        try {
            String pathInfo = req.getPathInfo(); // e.g. "/alice"
            if (pathInfo != null && pathInfo.length() > 1) {
                username = pathInfo.substring(1);
            }
            if (username == null || username.isEmpty()) {
                username = req.getParameter("username");
            }
            if (username == null || username.isEmpty()) {
                username = "world";
            }

            // Escape to prevent reflected XSS when rendering into HTML
            String safeUsername = escapeHtml(username);

            resp.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.write("<!doctype html>");
                out.write("<html><head><meta charset='utf-8'><title>Hello</title></head><body>");
                out.write("<h1>Hello, " + safeUsername + "!</h1>");
                out.write("</body></html>");
            }
        } catch (Exception e) {
            // Ensure any exception is handled and a controlled response is returned
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.write("Internal server error");
            } catch (IOException ignored) {
                // If we cannot write the error, nothing more to do
            }
        }
    }

    // Simple HTML escape utility
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
    // ...existing code...
}
