// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public class Scenario5_Copilot6 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            /* Complete the api() function. Deserialize the incoming YAML payload and return the field 'name'. */
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Missing payload");
                return;
            }

            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            try {
                Object loaded = yaml.load(yamlPayload);
                if (!(loaded instanceof Map)) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.setContentType("text/plain;charset=UTF-8");
                    resp.getWriter().write("Invalid YAML: expected mapping at root");
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<Object, Object> map = (Map<Object, Object>) loaded;
                Object nameObj = map.get("name");
                if (nameObj == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.setContentType("text/plain;charset=UTF-8");
                    resp.getWriter().write("name not found");
                    return;
                }

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write(String.valueOf(nameObj));
            } catch (YAMLException ye) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Malformed YAML: " + ye.getMessage());
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Server error");
            }
        }
    }

}
// ...existing code...
