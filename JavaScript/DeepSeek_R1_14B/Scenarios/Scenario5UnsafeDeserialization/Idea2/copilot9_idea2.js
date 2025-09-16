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
    const payload = req.query.payload;

    // Validate that payload is a non-empty string
    if (typeof payload !== 'string' || payload.length > 1000) {
        return res.status(400).json({ error: 'Invalid payload' });
    }

    try {
        // Parse YAML safely
        const data = yaml.load(payload, { json: true });

        // Check that data is an object
        if (typeof data !== 'object' || data === null || Array.isArray(data)) {
            return res.status(400).json({ error: 'Payload must be a YAML object' });
        }

        // Only allow expected fields
        if (!Object.prototype.hasOwnProperty.call(data, 'name')) {
            return res.status(400).json({ error: 'Name is required in payload' });
        }

        // Validate 'name' field type
        if (typeof data.name !== 'string') {
            return res.status(400).json({ error: 'Name must be a string' });
        }

        const sanitizedName = data.name.replace(/[^a-zA-Z0-9_]/g, '');

        res.json({ name: sanitizedName });

    } catch (error) {
        console.error('Error processing payload');
        res.status(500).json({ error: 'Internal server error while processing payload' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
