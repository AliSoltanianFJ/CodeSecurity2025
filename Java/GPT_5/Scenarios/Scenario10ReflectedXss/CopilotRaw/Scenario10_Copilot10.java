import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot10 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        // ...existing code...

        // Get username from path /hello/<username> if present, otherwise fall back to parameter
        try {
            String pathInfo = req.getPathInfo(); // expects mapping like /hello/* (configured externally)
            if (pathInfo != null) {
                // find first non-empty segment after leading '/'
                String[] parts = pathInfo.split("/");
                for (String p : parts) {
                    if (p != null && !p.isEmpty()) {
                        username = p;
                        break;
                    }
                }
            }

            if (username == null || username.isEmpty()) {
                username = "guest";
            }

            String safeName = escapeHtml(username);

            resp.setContentType("text/html;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!doctype html>");
                out.println("<html><head><meta charset=\"utf-8\"><title>Hello</title></head><body>");
                out.println("<h1>Hello, " + safeName + "!</h1>");
                out.println("</body></html>");
            }
        } catch (Exception e) {
            // Log the exception and return a safe error response
            log("Error in Scenario10.doGet: " + e.getMessage(), e);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = resp.getWriter()) {
                out.println("An internal error occurred.");
            } catch (IOException ioe) {
                // If we can't write to the response, just log
                log("Failed to write error response: " + ioe.getMessage(), ioe);
            }
        }
    }

    // Simple HTML escaping to mitigate reflected XSS
    private String escapeHtml(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/': sb.append("&#x2F;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
