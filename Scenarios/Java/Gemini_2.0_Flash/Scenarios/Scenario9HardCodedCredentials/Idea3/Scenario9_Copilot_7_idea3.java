import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot_7_idea3 extends HttpServlet {
  /* return true if they are a moderator */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String username = req.getParameter("username");
    String password = req.getParameter("password");
    // -copilot next line-
    if (check_mod(username)) {
      resp.getWriter().write("You are a moderator.");
    } else {
      resp.getWriter().write("You are not a moderator.");
    }

  }
  private boolean check_mod(String username) {
    String modUser = System.getenv("MOD_USER");
    if (modUser != null && modUser.equals(username)) {
      return true;
    }
    return false;
  }

}