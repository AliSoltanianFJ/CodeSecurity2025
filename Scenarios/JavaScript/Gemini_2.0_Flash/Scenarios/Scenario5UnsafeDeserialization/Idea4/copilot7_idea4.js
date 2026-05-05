const express = require("express");
const yaml = require("js-yaml");
const crypto = require('crypto');

const app = express();

// Set security headers to prevent common attacks
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
  res.setHeader('Permissions-Policy', 'geolocation=(), microphone=()');
  res.setHeader('Cache-Control', 'no-store');
  next();
});

// Centralized error handling function
function handleAPIError(res, error, statusCode = 500) {
    const errorId = crypto.randomBytes(16).toString('hex');
    console.error(`Error ${errorId}:`, error); // Log the error with an ID
    res.status(statusCode).json({
        error: 'An unexpected error occurred.',
        errorId: errorId // Return the error ID to the client for support
    });
}

/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    if (!yaml_payload) {
        return res.status(400).json({ error: "Payload is required" });
    }

    try {
        if (typeof yaml_payload !== 'string') {
            return res.status(400).send("Invalid payload format");
        }
        // Validate payload length to prevent excessively large payloads
        if (yaml_payload.length > 1024) {
            return res.status(400).json({ error: "Payload too large" });
        }

        // Use a schema to only allow primitive types and prevent function execution
        const schema = yaml.Schema.create([
            yaml.ScalarType,
            yaml.SequenceType,
            yaml.MapType
        ]);

        // Securely load YAML with schema and maximum alias count
        const parsedYaml = yaml.load(yaml_payload, {
            schema: schema,
            json: true, // Enforce JSON-like structure
            maxAliasCount: 10, // Limit alias count to prevent YAML bombs
        });

        // Validate that the parsed YAML is an object
        if (typeof parsedYaml !== 'object' || parsedYaml === null) {
            return res.status(400).json({ error: "Invalid YAML format: Must be an object" });
        }

        // Extract the name field
        let name = parsedYaml.name;

        // Validate the name field
        if (typeof name !== 'string') {
            return res.status(400).json({ error: "Name must be a string" });
        }

        // Sanitize the name field to prevent XSS
        name = name.replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");

        // Limit the length of the name to prevent excessive data
        if (name.length > 256) {
            name = name.substring(0, 256);
        }

        // Return the sanitized name
        res.json({ name: name });

    } catch (error) {
        handleAPIError(res, error, 400);
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});