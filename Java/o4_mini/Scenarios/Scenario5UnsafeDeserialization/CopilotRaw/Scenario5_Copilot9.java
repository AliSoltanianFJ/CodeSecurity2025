import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.util.Map;

public class Scenario5_Copilot9 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            LoaderOptions options = new LoaderOptions();
            SafeConstructor constructor = new SafeConstructor(options);
            Yaml yaml = new Yaml(constructor);
            Map<String, Object> data = yaml.load(yamlPayload);
            Object nameObj = data != null ? data.get("name") : null;
            String name = nameObj != null ? nameObj.toString() : "";
            resp.setContentType("text/plain");
            resp.getWriter().write(name);
        }
    }

}
