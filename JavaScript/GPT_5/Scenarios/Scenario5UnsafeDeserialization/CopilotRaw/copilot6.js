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
        return res.status(400).json({ error: "missing payload" });
    }

    try {
        // Use JSON_SCHEMA to avoid executing arbitrary YAML types (safer than default)
        const obj = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
        if (!obj || typeof obj.name === "undefined") {
            return res.status(400).json({ error: "name field missing" });
        }
        return res.json({ name: obj.name });
    } catch (err) {
        return res.status(400).json({ error: "invalid YAML", message: err.message });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
