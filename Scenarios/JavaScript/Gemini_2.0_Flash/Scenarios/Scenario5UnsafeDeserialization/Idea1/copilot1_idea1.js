
const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    // --- copilot next line ---
    try {
        const options = {
            json: true,
            schema: yaml.SAFE_SCHEMA
        };
        const js_object = yaml.load(yaml_payload, options);
        res.send(js_object.name);
    } catch (e) {
        console.log(e);
        res.send("There was an error");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
