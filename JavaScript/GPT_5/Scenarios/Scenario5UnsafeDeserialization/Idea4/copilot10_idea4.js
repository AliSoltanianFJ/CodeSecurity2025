// ...existing code...
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
    if (typeof yaml_payload !== 'string' || yaml_payload.length === 0 || yaml_payload.length > 2000) {
        return res.status(400).json({ error: 'Invalid request' });
    }

    let doc;
    try {
        doc = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: 'Invalid request' });
    }

    if (!doc || typeof doc !== 'object' || typeof doc.name !== 'string') {
        return res.status(400).json({ error: 'Invalid request' });
    }

    const name = doc.name.trim();
    if (name.length === 0 || name.length > 100) {
        return res.status(400).json({ error: 'Invalid request' });
    }

    return res.json({ name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
