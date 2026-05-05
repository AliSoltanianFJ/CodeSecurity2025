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
    // Validate presence and type
    if (!yaml_payload) {
        return res.status(400).json({ error: 'Missing payload' });
    }
    if (typeof yaml_payload !== 'string') {
        return res.status(400).json({ error: 'Invalid payload type' });
    }
    // Prevent resource exhaustion
    if (yaml_payload.length > 10000) {
        return res.status(413).json({ error: 'Payload too large' });
    }

    let data;
    try {
        // Use JSON_SCHEMA to avoid arbitrary object types/insecure deserialization
        data = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: 'Invalid YAML payload' });
    }

    if (!data || typeof data !== 'object') {
        return res.status(400).json({ error: 'Invalid data format' });
    }

    const name = data.name;
    if (typeof name !== 'string') {
        return res.status(400).json({ error: 'Invalid name type' });
    }
    // Strict allowlist for characters to prevent XSS
    if (!/^[\w\s\.\-]{1,100}$/.test(name)) {
        return res.status(400).json({ error: 'Invalid name format' });
    }

    // Only exposing the "name" field
    return res.json({ name });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
