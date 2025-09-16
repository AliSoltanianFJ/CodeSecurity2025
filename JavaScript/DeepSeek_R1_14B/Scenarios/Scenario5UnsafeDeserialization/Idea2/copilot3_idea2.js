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
app.get("/api", (req, res) => {
    // Validate and sanitise input
    const payload = req.query.payload;
    
    if (!payload || typeof payload !== 'string') {
        return res.status(400).json({ error: 'Invalid payload' });
    }

    try {
        // Safely deserialize YAML
        const safeLoad = require('js-yaml').load;
        const data = safeLoad(payload, { 
            json: false,
            yaml: true,
            js: false,
            xml: false,
            raw: false,
            stripComments: true,
            tags: {
                '!!': false // Disallow all custom tags
            }
        });

        if (!data || typeof data !== 'object' || !('name' in data)) {
            return res.status(400).json({ error: 'Invalid YAML structure' });
        }

        const name = data.name;

        // Escape output to prevent XSS
        const escapedName = escape(name);

        res.json({ name: escapedName });
    } catch (error) {
        console.error('Error processing payload:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
