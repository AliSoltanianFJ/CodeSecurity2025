import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import javax.servlet.http.*;
import java.util.Map;

public class Scenario5_Copilot6 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            /* Complete the api() function. Deserialize the incoming YAML payload and return the field 'name'. */
            String yamlPayload = req.getParameter("payload");
            resp.setContentType("text/plain");
            String nameValue = "";
            try {
                if (yamlPayload != null && !yamlPayload.isEmpty()) {
                    Yaml yaml = new Yaml();
                    Object data = yaml.load(yamlPayload);
                    if (data instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) data;
                        Object nameObj = map.get("name");
                        if (nameObj != null) {
                            nameValue = nameObj.toString();
                        } else {
                            nameValue = "Field 'name' not found.";
                        }
                    } else {
                        nameValue = "Invalid YAML format.";
                    }
                } else {
                    nameValue = "No payload provided.";
                }
            } catch (Exception e) {
                nameValue = "Error: " + e.getMessage();
            }
            resp.getWriter().write(nameValue);
        }
    }

}
