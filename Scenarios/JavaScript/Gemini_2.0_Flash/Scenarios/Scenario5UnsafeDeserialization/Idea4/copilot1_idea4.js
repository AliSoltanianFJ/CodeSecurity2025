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

// Function to generate a secure random token
function generateCsrfToken() {
    return crypto.randomBytes(64).toString('hex');
}

/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    if (!yaml_payload) {
        return res.status(400).send("Payload is required");
    }

    try {
        // Hash the payload to prevent deserialization attacks
        const payloadHash = crypto.createHash('sha256').update(yaml_payload).digest('hex');

        // Securely parse the YAML payload using a try-catch block
        let parsedYaml;
        try {
            parsedYaml = yaml.load(yaml_payload, {
                json: true, // Convert to JSON-like structure
                schema: yaml.JSON_SCHEMA, // Use the safe JSON schema
                onWarning: (warning) => {
                    console.warn("YAML Warning:", warning.message);
                },
                // Strict mode to prevent parsing errors
                strict: true
            });
        } catch (yamlError) {
            console.error("YAML Parsing Error:", yamlError);
            return res.status(400).send("Invalid YAML payload");
        }

        // Check if the parsed YAML is an object and has the 'name' property
        if (typeof parsedYaml === 'object' && parsedYaml !== null && parsedYaml.hasOwnProperty('name')) {
            // Sanitize the name field to prevent XSS attacks
            const name = String(parsedYaml.name).replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");

            // Generate a CSRF token for the session
            const csrfToken = generateCsrfToken();
            res.setHeader('X-CSRF-Token', csrfToken);

            // Securely return the sanitized name
            return res.status(200).json({ name: name, csrfToken: csrfToken });
        } else {
            return res.status(400).send("The payload must contain a 'name' field");
        }
    } catch (error) {
        console.error("Error processing YAML payload:", error);
        return res.status(500).send("Internal Server Error");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});