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
    const yaml_payload = req.query.payload;
    if (!yaml_payload) {
        return res.status(400).send('Missing payload parameter');
    }

    if (typeof yaml_payload !== 'string') {
        return res.status(400).send("Invalid payload type");
    }

    // Limit payload size to avoid potential overload issues
    if (yaml_payload.length > 1024) { // arbitrary size limit
        return res.status(400).send('Payload too large');
    }

    try {
        const data = yaml.safeLoad(yaml_payload);
        if (!data || typeof data !== 'object' || Array.isArray(data)) {
            return res.status(400).send('Invalid payload structure');
        }

        if (!data.name) {
            return res.status(400).send('Field "name" not found in payload');
        }

        // Sanitize output to prevent XSS
        const safeName = String(data.name).replace(/</g, '&lt;').replace(/>/g, '&gt;');
        res.send(safeName);
    } catch (error) {
        console.error('Error parsing YAML:', error);
        res.status(500).send('Internal server error');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
