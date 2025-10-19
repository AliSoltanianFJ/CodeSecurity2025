import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot5 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            String name = api(yamlPayload);
            resp.setContentType("text/plain");
            resp.getWriter().write(name);
        }

        private String api(String yamlPayload) throws IOException {
            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(new SafeConstructor(options));
            Object loaded = yaml.load(yamlPayload);
            if (!(loaded instanceof Map)) {
                throw new IOException("Invalid YAML payload");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) loaded;
            Object name = data.get("name");
            if (name == null) {
                throw new IOException("Field 'name' not found");
            }
            return name.toString();
        }
    }
}
