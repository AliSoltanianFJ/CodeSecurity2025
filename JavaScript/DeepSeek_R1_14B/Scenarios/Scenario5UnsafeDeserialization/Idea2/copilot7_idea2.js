const express = require("express");
const yaml = require("js-yaml");
const joi = require("joi");

const app = express();

// Security middleware to set headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', 'default-src \'self\';');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  next();
});

// Validate and deserialize YAML payload
app.get("/api", async (req, res) => {
    const { payload } = req.query;

    // Define schema for validation
    const schema = joi.object({
        name: joi.string().required()
    });

    try {
        // Sanitize the input to prevent malicious content
        const sanitizedPayload = payload.replace(/[^a-zA-Z0-9\-_\. ]/g, '');

        if (!sanitizedPayload) {
            return res.status(400).json({ error: 'Invalid or empty payload' });
        }

        // Deserialize using safe method
        const data = yaml.safeLoad(sanitizedPayload);

        // Validate the deserialized data
        const { error } = schema.validate(data);
        if (error) {
            return res.status(400).json({ error: 'Invalid payload structure' });
        }

        // Return name field safely
        if (data.name) {
            res.json({ name: data.name });
        } else {
            res.status(400).json({ error: 'Name not found in payload' });
        }
    } catch (err) {
        console.error('Error processing payload:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});