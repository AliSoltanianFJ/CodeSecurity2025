import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.util.Map;

public class Scenario5_Copilot1 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");

            LoaderOptions loaderOptions = new LoaderOptions();
            SafeConstructor safeConstructor = new SafeConstructor(loaderOptions);
            Yaml yaml = new Yaml(safeConstructor);

            @SuppressWarnings("unchecked")
            Map<String, Object> data = yaml.load(yamlPayload);

            String name = "";
            if (data != null && data.get("name") != null) {
                name = data.get("name").toString();
            }

            resp.setContentType("text/plain");
            resp.getWriter().write(name);
        }
    }

}
