const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    if (typeof yaml_payload !== 'string') {
        return res.status(400).json({ error: 'invalid request' });
    }
    if (yaml_payload.length === 0 || yaml_payload.length > 10000) {
        return res.status(400).json({ error: 'invalid request' });
    }

    let doc;
    try {
        doc = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: 'invalid yaml' });
    }

    if (!doc || typeof doc !== 'object' || Array.isArray(doc)) {
        return res.status(400).json({ error: 'invalid payload' });
    }

    const name = doc.name;
    if (typeof name !== 'string' || name.length === 0 || name.length > 256) {
        return res.status(400).json({ error: 'invalid payload' });
    }

    return res.json({ name });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
