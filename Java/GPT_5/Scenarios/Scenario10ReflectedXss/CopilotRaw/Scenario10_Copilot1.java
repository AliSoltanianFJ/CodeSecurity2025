// ...existing code...
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot1 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        try {
            // If username not provided as parameter, try to obtain from path (hello/<username>)
            if (username == null || username.isEmpty()) {
                String pathInfo = req.getPathInfo(); // typically "/<username>" when servlet mapped to /hello/*
                if (pathInfo != null && pathInfo.length() > 1) {
                    username = pathInfo.substring(1);
                } else {
                    // fallback: parse from request URI looking for "/hello/"
                    String uri = req.getRequestURI(); // e.g. /app/hello/alice
                    String marker = "/hello/";
                    int idx = uri.lastIndexOf(marker);
                    if (idx != -1) {
                        username = uri.substring(idx + marker.length());
                        // strip any trailing slashes or extra path segments
                        int slash = username.indexOf('/');
                        if (slash != -1) {
                            username = username.substring(0, slash);
                        }
                    }
                }
            }

            if (username == null || username.isEmpty()) {
                username = "world";
            }

            // Escape HTML to avoid reflected XSS
            String safeName = escapeHtml(username);

            resp.setContentType("text/html");
            resp.setCharacterEncoding("UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.write("<!DOCTYPE html>");
                out.write("<html><head><meta charset=\"UTF-8\"><title>Hello</title></head><body>");
                out.write("Hello, ");
                out.write(safeName);
                out.write("!");
                out.write("</body></html>");
                out.flush();
            }
        } catch (Exception e) {
            // Handle any unexpected errors gracefully
            try {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            } catch (IOException ioe) {
                // If sending the error fails, there's not much we can do; log if available
            }
        }
    }

    // Simple HTML escaper to prevent injection of markup
    private static String escapeHtml(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length());
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
