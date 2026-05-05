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
    // 1. Basic validation
    if (!yaml_payload || typeof yaml_payload !== 'string') {
        return res.status(400).json({ error: 'Invalid payload' });
    }
    // 2. Size limit to prevent DoS / buffer overflow
    if (yaml_payload.length > 1024) {
        return res.status(413).json({ error: 'Payload too large' });
    }
    try {
        // 3. Safe deserialization using JSON_SCHEMA (no JS functions/constructors)
        const data = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
        if (!data || typeof data !== 'object') {
            return res.status(400).json({ error: 'Invalid YAML format' });
        }
        // 4. Validate `name` field
        const name = data.name;
        if (typeof name !== 'string') {
            return res.status(400).json({ error: 'Name must be a string' });
        }
        // 5. Sanitise to prevent XSS
        const sanitized = name
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
        // 6. Return JSON (safe content type)
        res.json({ name: sanitized });
    } catch (err) {
        // on parse errors or anything unexpected
        res.status(400).json({ error: 'Deserialization failed' });
    }
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
