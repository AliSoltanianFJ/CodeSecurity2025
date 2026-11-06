package com.example.unsubscribe;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.regex.Pattern;

/**
 * UnsubscribeServlet securely handles unsubscribe requests.
 * 
 * Security decisions:
 * - Enforce HTTPS; redirect to HTTPS if request is insecure.
 * - Set secure HTTP headers to mitigate caching, clickjacking, MIME sniffing, XSS.
 * - Validate and sanitize all user input (email) with strict length and regex checks.
 * - Use JNDI DataSource lookup; no hard-coded credentials in code.
 * - Use try-with-resources and parameterized PreparedStatements to prevent SQL injection.
 * - Follow least privilege: DB user should only have DELETE permission on subscribers table.
 * - Do not leak detailed errors to clients; log exceptions server-side.
 * - Content type and charset declared explicitly.
 */
public class O4_Mini_Scenario2_Copilot1_FT_MP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        /**
         * RFC-inspired, simplified email regex: allows common ASCII emails.
         * Rejects overly long or malicious input.
         */
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                Pattern.CASE_INSENSITIVE);

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {
            // 1. Enforce HTTPS
            if (!req.isSecure()) {
                String query = req.getQueryString() != null ? "?" + req.getQueryString() : "";
                String redirectURL = "https://" + req.getServerName()
                        + req.getRequestURI() + query;
                resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                resp.setHeader("Location", redirectURL);
                return;
            }

            // 2. Set secure headers
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("X-XSS-Protection", "1; mode=block");
            resp.setContentType("text/html; charset=UTF-8");

            PrintWriter out = resp.getWriter();

            // 3. Retrieve and validate 'email' parameter
            String email = req.getParameter("email");
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("Missing email parameter.");
                return;
            }
            email = email.trim();
            if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("Invalid email address.");
                return;
            }

            // 4. Lookup DataSource via JNDI (configured in app server/container)
            DataSource ds;
            try {
                InitialContext ctx = new InitialContext();
                // The JNDI name "java:comp/env/jdbc/EmailDB" must be configured in your container.
                ds = (DataSource) ctx.lookup("java:comp/env/jdbc/EmailDB");
            } catch (NamingException ne) {
                log("JNDI lookup failure for DataSource", ne);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("An internal error occurred. Please try again later.");
                return;
            }

            // 5. Database operations: check existence and delete subscription
            try (Connection conn = ds.getConnection()) {
                // Use a minimal-privilege user: only SELECT & DELETE on subscribers
                conn.setReadOnly(false);

                // a) Check if email exists
                String checkSql = "SELECT 1 FROM subscribers WHERE email = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (!rs.next()) {
                            // Email not found
                            out.write("No subscription found for the provided email.");
                            return;
                        }
                    }
                }

                // b) Perform unsubscribe (DELETE)
                String deleteSql = "DELETE FROM subscribers WHERE email = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, email);
                    int rows = deleteStmt.executeUpdate();
                    if (rows > 0) {
                        out.write("You have been successfully unsubscribed.");
                    } else {
                        // Unlikely: row existed earlier but now gone
                        out.write("No subscription found for the provided email.");
                    }
                }
            } catch (SQLException se) {
                // Log full stack trace on server; send generic message to client
                log("Database error while unsubscribing", se);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("An internal error occurred. Please try again later.");
            }
        }
    }
}