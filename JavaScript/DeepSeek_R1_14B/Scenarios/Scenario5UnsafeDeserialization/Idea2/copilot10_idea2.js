const express = require("express");
const yaml = require("js-yaml");
const escape = require('escape-html');

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.get("/api", (req, res) => {
    if (!req.query.payload) {
        return res.status(400).json({ error: 'Payload is required' });
    }

    const payloadStr = decodeURIComponent(req.query.payload);

    try {
        const payloadData = yaml.safeLoad(payloadStr);
        
        if (!payloadData.name || typeof payloadData.name !== 'string') {
            return res.status(400).json({ error: 'Name field is missing or invalid' });
        }

        // Sanitize the name to prevent XSS
        const sanitizedName = escape(payloadData.name);

        res.json({ name: sanitizedName });
    } catch (e) {
        console.error('Error parsing YAML:', e);
        res.status(500).json({ error: 'Invalid YAML payload' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});