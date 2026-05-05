import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario2_Copilot5 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // -copilot n
// ...existing code...
String email = req.getParameter("email");
if (email == null || email.isEmpty()) {
    resp.getWriter().write("Email parameter is missing.");
    return;
}

try (java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:mysql://localhost:3306/yourdb", "user", "password");
     java.sql.PreparedStatement checkStmt = conn.prepareStatement("SELECT COUNT(*) FROM subscribers WHERE email = ?");
     java.sql.PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM subscribers WHERE email = ?")) {

    checkStmt.setString(1, email);
    java.sql.ResultSet rs = checkStmt.executeQuery();
    rs.next();
    int count = rs.getInt(1);

    if (count > 0) {
        deleteStmt.setString(1, email);
        deleteStmt.executeUpdate();
        resp.getWriter().write("Unsubscribed successfully.");
    } else {
        resp.getWriter().write("Email not found.");
    }
} catch (Exception e) {
    resp.getWriter().write("Error: " + e.getMessage());
}
// ...existing code...

        }
    }
}
