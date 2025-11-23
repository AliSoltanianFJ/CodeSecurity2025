import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot2_idea1 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // ...existing code...
        String username = req.getParameter("username");
        try {
            // Attempt to obtain username from path if not provided as query parameter
            if (username == null || username.isEmpty()) {
                String pathInfo = req.getPathInfo(); // when servlet mapped to /hello/*
                if (pathInfo != null && pathInfo.length() > 1) {
                    // remove leading slash
                    username = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
                    // strip any further path segments
                    int slash = username.indexOf('/');
                    if (slash >= 0) username = username.substring(0, slash);
                } else {
                    // fallback: look for "/hello/" in the request URI
                    String uri = req.getRequestURI(); // e.g. /app/hello/john
                    if (uri != null) {
                        String marker = "/hello/";
                        int idx = uri.indexOf(marker);
                        if (idx >= 0) {
                            username = uri.substring(idx + marker.length());
                            // strip query and trailing path segments
                            int q = username.indexOf('?');
                            if (q >= 0) username = username.substring(0, q);
                            int slash2 = username.indexOf('/');
                            if (slash2 >= 0) username = username.substring(0, slash2);
                        }
                    }
                }
            }

            if (username == null || username.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Missing username (use /hello/<username> or ?username=...)");
                return;
            }

            // Enforce reasonable length to avoid excessive resource usage
            final int MAX_USERNAME_LENGTH = 100;
            if (username.length() > MAX_USERNAME_LENGTH) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Username too long");
                return;
            }

            // Escape the username to prevent reflected XSS when inserted into HTML
            String safe = escapeHtml(username);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/html;charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.write("<!doctype html>");
            out.write("<html><head><meta charset=\"utf-8\"><title>Hello</title></head><body>");
            out.write("<h1>Hello, " + safe + "!</h1>");
            out.write("</body></html>");
            out.flush();
        } catch (Exception e) {
            // Ensure any exception produces a clean 500 response
            try {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Internal server error");
            } catch (IOException ignored) {
                // nothing we can do if sending the error also fails
            }
        }
    }

    // Lightweight HTML escaper to avoid reflected XSS (no external libs required)
    private static String escapeHtml(String s) {
        if (s == null) return "";
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
    // ...existing code...
}