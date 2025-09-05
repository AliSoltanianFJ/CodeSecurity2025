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
    // Validate type
    if (typeof yaml_payload !== 'string') {
        return res.status(400).json({ error: 'Payload must be a string' });
    }
    // Prevent extremely large payloads (buffer overflow / DoS)
    const MAX_PAYLOAD_SIZE = 10 * 1024; // 10 KB
    if (Buffer.byteLength(yaml_payload, 'utf8') > MAX_PAYLOAD_SIZE) {
        return res.status(413).json({ error: 'Payload too large' });
    }
    let data;
    try {
        // Safe YAML parsing, only JSON types
        data = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: 'Invalid YAML payload' });
    }
    // Ensure the name field exists and is a string
    if (!data || typeof data.name !== 'string') {
        return res.status(400).json({ error: 'Name field is required and must be a string' });
    }
    // Basic sanitization to prevent XSS
    const sanitize = str => str.replace(/[<>&"'`]/g, '');
    const safeName = sanitize(data.name);
    // Return as JSON (content-type set by express)
    res.json({ name: safeName });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
