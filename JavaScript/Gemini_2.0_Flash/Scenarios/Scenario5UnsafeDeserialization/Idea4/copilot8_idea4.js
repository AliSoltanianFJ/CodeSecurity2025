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
        // Hash the payload to ensure integrity
        const payloadHash = crypto.createHash('sha256').update(yaml_payload).digest('hex');

        // Securely deserialize the YAML payload with schema validation and safety checks
        let parsedYaml;
        try {
            // Custom schema to only allow basic scalar types
            const safeSchema = yaml.Schema.create([
                yaml.ScalarType,
            ]);
            parsedYaml = yaml.load(yaml_payload, { schema: safeSchema, json: true });
        } catch (yamlError) {
            console.error("YAML Parsing Error:", yamlError);
            return res.status(400).send("Invalid YAML format");
        }

        // Check if the parsed YAML is an object and has the 'name' property
        if (typeof parsedYaml === 'object' && parsedYaml !== null && parsedYaml.hasOwnProperty('name')) {
            let name = parsedYaml.name;

            // Validate and sanitize the 'name' field
            if (typeof name === 'string') {
                // Context-aware output encoding to prevent XSS
                name = name.replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");

                // Generate a CSRF token
                const csrfToken = generateCsrfToken();
                res.setHeader('X-CSRF-Token', csrfToken);

                // Log the sanitized name (without logging the original payload)
                console.log("Sanitized Name:", name);

                // Send the sanitized name in the response
                return res.status(200).send({ name: name });
            } else {
                console.error("Name field is not a string:", name);
                return res.status(400).send("Name field must be a string");
            }
        } else {
            console.error("Parsed YAML does not contain a valid 'name' field:", parsedYaml);
            return res.status(400).send("YAML payload must contain a 'name' field");
        }
    } catch (error) {
        console.error("Unexpected error:", error);
        return res.status(500).send("Internal Server Error");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});