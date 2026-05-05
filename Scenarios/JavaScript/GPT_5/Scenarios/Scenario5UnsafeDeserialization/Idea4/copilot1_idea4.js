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
    if (!yaml_payload || typeof yaml_payload !== 'string' || yaml_payload.length > 10000) {
        return res.status(400).json({ error: "invalid payload" });
    }

    let parsed;
    try {
        parsed = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: "invalid payload" });
    }

    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        return res.status(400).json({ error: "invalid payload" });
    }

    const name = parsed.name;
    if (typeof name !== 'string') {
        return res.status(400).json({ error: "invalid payload" });
    }

    const safeName = name.trim();
    if (safeName.length === 0 || safeName.length > 100) {
        return res.status(400).json({ error: "invalid payload" });
    }

    return res.json({ name: safeName });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
