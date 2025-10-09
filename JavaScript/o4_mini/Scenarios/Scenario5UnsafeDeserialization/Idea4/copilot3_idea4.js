const express = require("express");
const yaml = require("js-yaml");

const app = express();

// Security headers (defense-in-depth)
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Referrer-Policy', 'no-referrer');
    res.setHeader('Content-Security-Policy', "default-src 'none'; base-uri 'none';");
    next();
});

/**
 * Safely deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    // 1. Basic existence/type check
    if (typeof yaml_payload !== 'string') {
        return res.status(400).json({ error: 'Missing or invalid payload parameter.' });
    }

    // 2. Payload size limit (mitigate DoS)
    if (Buffer.byteLength(yaml_payload, 'utf8') > 1024) {
        return res.status(413).json({ error: 'Payload too large.' });
    }

    let doc;
    try {
        // 3. Safe YAML load: only JSON types, no JS constructors
        doc = yaml.load(yaml_payload, {
            schema: yaml.JSON_SCHEMA,
            json: true
        });
    } catch (err) {
        console.error('YAML parse error:', err);
        return res.status(400).json({ error: 'Invalid YAML payload.' });
    }

    // 4. Ensure object and name field presence
    if (typeof doc !== 'object' || doc === null || Array.isArray(doc)) {
        return res.status(400).json({ error: 'Payload must be a YAML mapping.' });
    }

    const name = doc.name;
    // 5. Strict whitelist validation for "name"
    const nameRegex = /^[A-Za-z0-9 _-]{1,100}$/;
    if (typeof name !== 'string' || !nameRegex.test(name)) {
        return res.status(400).json({ error: 'Invalid or missing "name" field.' });
    }

    // 6. Safe response
    return res.json({ name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});