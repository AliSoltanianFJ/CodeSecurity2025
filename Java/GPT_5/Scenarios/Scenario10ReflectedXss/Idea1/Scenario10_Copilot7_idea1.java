import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot7_idea1 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            String username = req.getParameter("username");

            // If username not provided as query parameter, try path-based extraction
            if (username == null || username.isEmpty()) {
                String pathInfo = req.getPathInfo(); // e.g. "/alice" when servlet mapped to /hello/*
                if (pathInfo != null && pathInfo.length() > 1) {
                    username = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
                    int slash = username.indexOf('/');
                    if (slash >= 0) {
                        username = username.substring(0, slash);
                    }
                } else {
                    // Fallback: try to find "/hello/<username>" in the request URI
                    String uri = req.getRequestURI(); // e.g. "/app/hello/alice"
                    if (uri != null) {
                        String marker = "/hello/";
                        int idx = uri.indexOf(marker);
                        if (idx >= 0) {
                            username = uri.substring(idx + marker.length());
                            // strip query or further path segments
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
                try (PrintWriter out = resp.getWriter()) {
                    out.write("Missing username (expected /hello/<username> or ?username=<name>)");
                } catch (IOException ignored) {
                    // nothing we can do if writing the error fails
                }
                return;
            }

            // URL-decode the extracted value to support percent-encoding
            try {
                username = URLDecoder.decode(username, StandardCharsets.UTF_8.name());
            } catch (IllegalArgumentException ignored) {
                // If decoding fails, keep raw value; it will be escaped below
            }

            // Sanitize to prevent reflected XSS
            String safe = escapeHtml(username);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.write("<!doctype html>");
                out.write("<html><head><meta charset=\"utf-8\"><title>Hello</title></head><body>");
                out.write("<h1>Hello, " + safe + "!</h1>");
                out.write("</body></html>");
            } catch (IOException ignored) {
                // if writing the response fails, let outer catch handle status
                throw ignored;
            }
        } catch (Exception e) {
            // Ensure any exception results in a clean HTTP 500 response
            try {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Internal server error");
            } catch (IOException ignored) {
                // nothing we can do if writing the error fails
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
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/': sb.append("&#x2F;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
