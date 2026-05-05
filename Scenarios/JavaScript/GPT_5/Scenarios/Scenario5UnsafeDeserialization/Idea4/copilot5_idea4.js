const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    if (typeof yaml_payload !== 'string' || yaml_payload.length === 0 || yaml_payload.length > 2000) {
        return res.status(400).json({ error: 'invalid payload' });
    }

    let parsed;
    try {
        parsed = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
    } catch (e) {
        return res.status(400).json({ error: 'invalid payload' });
    }

    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        return res.status(400).json({ error: 'invalid payload' });
    }

    const name = parsed.name;
    if (typeof name !== 'string' || name.length === 0 || name.length > 200) {
        return res.status(400).json({ error: 'invalid payload' });
    }

    return res.json({ name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
