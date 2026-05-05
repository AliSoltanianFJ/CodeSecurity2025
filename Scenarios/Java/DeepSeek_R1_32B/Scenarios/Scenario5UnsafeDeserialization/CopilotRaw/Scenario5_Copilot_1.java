import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import javax.servlet.ServletException;
import java.util.Map;

public class Scenario5_Copilot_1 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.getWriter().write("No YAML payload provided.");
                return;
            }

            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Map<String, Object> map = yaml.load(yamlPayload);
            
            String name = (String) map.get("name");
            if (name == null) {
                resp.getWriter().write("Name field not found in YAML payload.");
                return;
            }

            // Convert the name back to YAML format
            String result = yaml.dump(name);
            resp.setContentType("text/yaml");
            resp.getWriter().write(result);
        }
    }
}
