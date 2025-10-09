const express = require("express");
const yaml = require("js-yaml");
const crypto = require('crypto');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Securely parse JSON
function safeJsonParse(data) {
    try {
        return JSON.parse(data);
    } catch (error) {
        // Log the error (without including the data)
        console.error("Error parsing JSON");
        return null;
    }
}

/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    if (!yaml_payload) {
        return res.status(400).send("Payload is required");
    }

    // Generate a unique identifier for this request
    const requestId = crypto.randomBytes(16).toString("hex");

    try {
        if (typeof yaml_payload !== 'string') {
            return res.status(400).send("Invalid payload format");
        }
        // Validate payload length
        if (yaml_payload.length > 2048) {
            console.warn(`Request ${requestId}: Payload length exceeds maximum allowed.`);
            return res.status(400).send("Payload too large");
        }

        // Sanitize the YAML payload by escaping potentially harmful characters
        const sanitizedYamlPayload = yaml_payload.replace(/</g, "&lt;").replace(/>/g, "&gt;");

        // Attempt to parse the YAML payload
        let parsedYaml;
        try {
            parsedYaml = yaml.load(sanitizedYamlPayload, { json: true });
        } catch (yamlError) {
            console.error(`Request ${requestId}: YAML parsing error:`, yamlError);
            return res.status(400).send("Invalid YAML format");
        }

        // Check if the parsed YAML is an object
        if (typeof parsedYaml !== 'object' || parsedYaml === null) {
            console.warn(`Request ${requestId}: Parsed YAML is not an object.`);
            return res.status(400).send("YAML must represent an object");
        }

        // Check if the 'name' field exists
        if (!parsedYaml.hasOwnProperty('name')) {
            console.warn(`Request ${requestId}: 'name' field is missing in YAML.`);
            return res.status(400).send("The 'name' field is required");
        }

        // Extract the 'name' field
        let name = parsedYaml.name;

        // Validate the 'name' field
        if (typeof name !== 'string') {
            console.warn(`Request ${requestId}: 'name' field is not a string.`);
            return res.status(400).send("The 'name' field must be a string");
        }

        // Sanitize the name field to prevent XSS
        const sanitizedName = name.replace(/</g, "&lt;").replace(/>/g, "&gt;");

        // Limit the length of the name to prevent excessive data
        if (sanitizedName.length > 128) {
            console.warn(`Request ${requestId}: 'name' field exceeds maximum length.`);
            return res.status(400).send("Name too long");
        }

        // Return the sanitized name
        res.status(200).send({ name: sanitizedName });

    } catch (error) {
        console.error(`Request ${requestId}: An unexpected error occurred:`, error);
        res.status(500).send("An unexpected error occurred");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});