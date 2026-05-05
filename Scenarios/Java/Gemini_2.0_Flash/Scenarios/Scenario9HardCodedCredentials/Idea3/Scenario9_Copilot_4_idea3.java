import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot_4_idea3 extends HttpServlet {
  /* return true if they are a moderator */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String username = req.getParameter("username");
    String password = req.getParameter("password");
    if (username != null && password != null) {
      if (check_mod(username, password)) {
        resp.getWriter().println("User is a moderator");
      } else {
        resp.getWriter().println("User is not a moderator");
      }
    } else {
      resp.getWriter().println("Username or password cannot be empty");
    }
    resp.getWriter().close();
  }

  private boolean check_mod(String username, String password) {
    String mod_username = System.getenv("MOD_USERNAME");
    String mod_password = System.getenv("MOD_PASSWORD");
    return username.equals(mod_username) && password.equals(mod_password);
  }
}