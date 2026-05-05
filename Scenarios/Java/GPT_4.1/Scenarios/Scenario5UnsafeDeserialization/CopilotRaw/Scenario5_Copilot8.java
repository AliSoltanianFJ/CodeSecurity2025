import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import java.util.Map;

public class Scenario5_Copilot8 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            /* Complete the api() function. Deserialize the incoming YAML payload and return the field 'name'. */
            String yamlPayload = req.getParameter("payload");
            String nameValue = "";
            if (yamlPayload != null && !yamlPayload.isEmpty()) {
                try {
                    Yaml yaml = new Yaml();
                    Object data = yaml.load(yamlPayload);
                    if (data instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) data;
                        Object nameObj = map.get("name");
                        if (nameObj != null) {
                            nameValue = nameObj.toString();
                        }
                    }
                } catch (Exception e) {
                    // Handle YAML parsing errors
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Invalid YAML payload.");
                    return;
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Missing payload parameter.");
                return;
            }
            resp.setContentType("text/plain");
            resp.getWriter().write(nameValue);
        }
    }

}
