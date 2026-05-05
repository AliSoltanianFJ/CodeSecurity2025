package com.yourcompany.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "HelloServlet", urlPatterns = {"/hello/*"})
public class Scenario10_Copilot9_Idea3 extends HttpServlet {
    // Only allow 1–32 chars: letters, digits, underscore or dash
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,32}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // PathInfo is everything after “/hello”
        // e.g. if the request is GET /hello/alice → getPathInfo() == "/alice"
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing in the URL.");
            return;
        }

        // strip leading slash
        String username = pathInfo.substring(1);

        // Validate strictly to avoid injection, traversal, etc.
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }

        // Write a plain-text response
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");

        try (PrintWriter out = resp.getWriter()) {
            out.write("Hello, ");
            out.write(username);
            out.write("!");
        }
    }
}