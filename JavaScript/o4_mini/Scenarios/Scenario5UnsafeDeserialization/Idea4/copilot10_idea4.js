const express = require("express");
const yaml = require("js-yaml");

const app = express();

// Defense-in-depth HTTP headers
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
    next();
});

/**
 * Safely deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const { payload } = req.query;

    // 1. Input validation: must be a non-empty string and under 10 000 chars
    if (typeof payload !== 'string' || payload.length === 0 || payload.length > 10000) {
        return res.status(400).json({ error: 'Invalid payload format or size.' });
    }

    let doc;
    try {
        // 2. Safe YAML deserialization: forbid arbitrary types/tags
        doc = yaml.load(payload, { schema: yaml.JSON_SCHEMA });
    } catch (e) {
        return res.status(400).json({ error: 'Malformed YAML payload.' });
    }

    // 3. Post-deserialization checks
    if (!doc || typeof doc !== 'object') {
        return res.status(400).json({ error: 'YAML did not produce an object.' });
    }

    // 4. Whitelist and validate "name" field
    const name = doc.name;
    if (typeof name !== 'string' || name.length === 0 || name.length > 100) {
        return res.status(400).json({ error: 'Missing or invalid "name" field.' });
    }

    // 5. Simple sanitization: strip control characters
    const sanitized = name.replace(/[\u0000-\u001F\u007F]/g, '');

    // 6. Return JSON safely
    res.json({ name: sanitized });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});