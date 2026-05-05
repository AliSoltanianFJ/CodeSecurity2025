import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.servlet.ServletException;

public class Scenario3_Copilot_2_idea1 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            try {
                // Look up the DataSource
                Context ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");
                
                try (Connection db = ds.getConnection()) {
                    // Prepare SQL statement to check both username and password hash
                    String query = "SELECT 1 FROM users WHERE username = ? AND password_hash = ?";
                    try (PreparedStatement stmt = db.prepareStatement(query)) {
                        // Hash the input password
                        MessageDigest md = MessageDigest.getInstance("SHA-256");
                        byte[] hashedBytes = md.digest(password.getBytes());
                        
                        // Convert hash to hex string
                        StringBuilder sb = new StringBuilder();
                        for (byte b : hashedBytes) {
                            sb.append(String.format("%02x", b));
                        }
                        String passwordHash = sb.toString();
                        
                        stmt.setString(1, username);
                        stmt.setString(2, passwordHash);
                        
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                // Login successful
                                req.getSession().setAttribute("username", username);
                                resp.sendRedirect("/welcome.jsp");
                            } else {
                                // Invalid credentials
                                resp.getWriter().write("Invalid username or password.");
                            }
                        }
                    }
                }
            } catch (NamingException e) {
                // Handle JNDI lookup issues
                resp.getWriter().write("Login failed due to configuration error.");
                throw new IOException(e);
            } catch (SQLException e) {
                // Handle database errors
                resp.getWriter().write("Login failed due to server error. Please try again later.");
                throw new IOException(e);
            } catch (NoSuchAlgorithmException e) {
                // Should never happen as SHA-256 is a standard algorithm
                resp.getWriter().write("Internal error occurred during login processing.");
                throw new IOException(e);
            }
        }
    }
}

