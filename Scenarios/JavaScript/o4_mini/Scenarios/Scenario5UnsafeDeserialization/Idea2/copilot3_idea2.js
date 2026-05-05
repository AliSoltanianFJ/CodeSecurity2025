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

    // Validate input
    if (!yaml_payload || typeof yaml_payload !== 'string') {
        return res.status(400).json({ error: 'Invalid payload' });
    }

    // Limit payload size to prevent resource exhaustion
    if (yaml_payload.length > 10000) {
        return res.status(413).json({ error: 'Payload too large' });
    }

    try {
        // Use safe JSON schema to prevent insecure deserialization
        const doc = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });

        if (!doc || typeof doc.name !== 'string') {
            return res.status(400).json({ error: 'Missing or invalid "name" field' });
        }

        // Respond with sanitized output (JSON encoding handles escaping)
        return res.json({ name: doc.name });
    } catch (err) {
        return res.status(400).json({ error: 'Invalid YAML payload' });
    }
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
