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
    console.error(`Error ${errorId}:`, error); // Log the full error with a unique ID
    res.status(statusCode).json({
        error: 'An unexpected error occurred.',
        errorId: errorId, // Return the error ID to the client for support purposes
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

        // Hash the payload for integrity checks
        const payloadHash = crypto.createHash('sha256').update(yaml_payload).digest('hex');

        let parsedYaml;
        try {
            // Secure YAML parsing options to prevent code execution
            parsedYaml = yaml.load(yaml_payload, {
                json: true, // Force JSON-compatible mode
                schema: yaml.JSON_SCHEMA, // Use the safe JSON schema
                onWarning: (warning) => {
                    console.warn("YAML Warning:", warning); // Log any warnings during parsing
                }
            });
        } catch (yamlError) {
            console.error("YAML Parsing Error:", yamlError);
            return res.status(400).json({ error: "Invalid YAML format" });
        }

        // Check if parsedYaml is null or undefined
        if (!parsedYaml || typeof parsedYaml !== 'object') {
            return res.status(400).json({ error: "Invalid YAML content" });
        }

        // Extract the name field
        let name = parsedYaml.name;

        // Validate the extracted name
        if (typeof name !== 'string') {
            return res.status(400).json({ error: "Name field is not a string" });
        }

        // Sanitize the name field to prevent XSS
        name = name.replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");

        // Log the successful operation (without sensitive data)
        console.log(`Successfully processed payload with hash: ${payloadHash}`);

        // Send the sanitized name in the response
        res.json({ name: name });

    } catch (error) {
        handleAPIError(res, error);
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});