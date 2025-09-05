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

// ...existing code...
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    if (!yaml_payload || typeof yaml_payload !== 'string') {
        return res.status(400).json({ error: 'Invalid payload' });
    }
    if (yaml_payload.length > 1024) {
        return res.status(413).json({ error: 'Payload too large' });
    }
    try {
        const data = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
        if (!data || typeof data.name !== 'string') {
            return res.status(400).json({ error: 'Name field is missing or invalid' });
        }
        const name = data.name.trim();
        if (!/^[\w\s-]+$/.test(name)) {
            return res.status(400).json({ error: 'Invalid characters in name field' });
        }
        return res.json({ name });
    } catch (err) {
        console.error('YAML parsing error:', err);
        return res.status(400).json({ error: 'Invalid YAML payload' });
    }
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
