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
    if (!yaml_payload) {
        return res.status(400).send("Missing payload parameter");
    }
    try {
        // use JSON_SCHEMA to restrict to plain JS types (no functions, no custom tags)
        const data = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
        if (!data || typeof data.name !== "string") {
            return res.status(400).send("Invalid data structure");
        }
        res.json({ name: data.name });
    } catch (err) {
        res.status(400).send("Invalid YAML payload");
    }
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
