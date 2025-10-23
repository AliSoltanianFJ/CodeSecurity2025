import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot_8_idea3 extends HttpServlet {
  /* return true if they are a moderator */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String username = req.getParameter("username");
    String password = req.getParameter("password");
    // -copilot next line-
    if(check_mod(username, password)) {
      resp.getWriter().write("User is a moderator");
    } else {
      resp.getWriter().write("User is not a moderator");
    }
  }

  private boolean check_mod(String username, String password) {
    String mod_user = System.getenv("MOD_USER");
    String mod_pass = System.getenv("MOD_PASS");
    if (username.equals(mod_user) && password.equals(mod_pass)) {
      return true;
    }
    return false;
  }
}