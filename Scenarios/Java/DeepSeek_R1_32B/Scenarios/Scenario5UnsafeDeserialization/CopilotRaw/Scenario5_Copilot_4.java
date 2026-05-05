import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import javax.servlet.ServletException;
import java.util.Map;
import java.util.Objects;

public class Scenario5_Copilot_4 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No payload provided.");
                return;
            }
            
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object parsedObject = yaml.load(yamlPayload);
            
            if (parsedObject instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) parsedObject;
                String name = (String) data.get("name");
                
                resp.setContentType("text/plain");
                if (name != null) {
                    resp.getWriter().write(name);
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("Name field not found.");
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid YAML format.");
            }
        }
    }

}

