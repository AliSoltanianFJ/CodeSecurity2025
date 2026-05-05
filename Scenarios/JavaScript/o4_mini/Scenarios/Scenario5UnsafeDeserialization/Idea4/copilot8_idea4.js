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
    if (typeof yaml_payload !== 'string' || yaml_payload.length > 1024) {
        return res.status(400).json({ error: 'Invalid payload' });
    }
    let parsed;
    try {
        parsed = yaml.load(yaml_payload, { schema: yaml.FAILSAFE_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: 'Malformed YAML' });
    }
    const name = typeof parsed?.name === 'string' ? parsed.name : '';
    res.json({ name });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
