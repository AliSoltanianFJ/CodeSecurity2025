import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot2 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Read YAML payload from the 'payload' query parameter
            String yamlPayload = req.getParameter("payload");

            // Configure SnakeYAML to use a safe constructor
            LoaderOptions loaderOptions = new LoaderOptions();
            SafeConstructor safeConstructor = new SafeConstructor(loaderOptions);
            Yaml yaml = new Yaml(safeConstructor);

            // Deserialize into a Map
            @SuppressWarnings("unchecked")
            Map<String, Object> data = yaml.load(yamlPayload);

            // Extract the 'name' field
            String name = "";
            if (data != null && data.containsKey("name")) {
                Object nameObj = data.get("name");
                name = nameObj != null ? nameObj.toString() : "";
            }

            // Return the 'name' value
            resp.setContentType("text/plain");
            resp.getWriter().write(name);
        }
    }
}
