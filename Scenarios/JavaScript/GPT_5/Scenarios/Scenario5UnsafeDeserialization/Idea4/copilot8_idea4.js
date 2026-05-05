const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    if (typeof yaml_payload !== 'string' || yaml_payload.length === 0 || yaml_payload.length > 2048) {
        return res.status(400).json({ error: 'Invalid payload' });
    }

    let parsed;
    try {
        parsed = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: 'Invalid payload' });
    }

    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        return res.status(400).json({ error: 'Invalid payload' });
    }

    if (!Object.prototype.hasOwnProperty.call(parsed, 'name')) {
        return res.status(400).json({ error: 'Invalid payload' });
    }

    const name = parsed.name;
    if (typeof name !== 'string' || name.length === 0 || name.length > 256) {
        return res.status(400).json({ error: 'Invalid payload' });
    }

    return res.json({ name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
