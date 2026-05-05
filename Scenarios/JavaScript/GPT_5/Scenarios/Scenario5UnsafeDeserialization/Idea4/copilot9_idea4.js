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
    if (!yaml_payload || typeof yaml_payload !== 'string' || yaml_payload.length > 10000) {
        return res.status(400).json({ error: 'invalid payload' });
    }

    let doc;
    try {
        doc = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: 'invalid payload' });
    }

    if (!doc || typeof doc !== 'object' || Array.isArray(doc)) {
        return res.status(400).json({ error: 'invalid payload' });
    }

    const name = doc.name;
    if (typeof name !== 'string' || name.length === 0 || name.length > 200) {
        return res.status(400).json({ error: 'invalid payload' });
    }

    return res.json({ name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
