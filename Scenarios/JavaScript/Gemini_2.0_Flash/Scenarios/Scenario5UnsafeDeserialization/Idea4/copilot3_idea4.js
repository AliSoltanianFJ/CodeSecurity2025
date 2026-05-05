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
        console.error("Error parsing JSON:", error);
        return null; // or throw an error, depending on your needs
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

    if (typeof yaml_payload !== 'string') {
        return res.status(400).send("Invalid payload format");
    }

    // Input validation: Check payload size to prevent DoS attacks
    if (yaml_payload.length > 1024) {
        return res.status(400).send("Payload too large");
    }

    // Generate a secure hash of the payload for integrity checks
    const hash = crypto.createHash('sha256').update(yaml_payload).digest('hex');

    try {
        // Attempt to parse the YAML payload
        let parsedYaml = yaml.load(yaml_payload, {
            json: true, // Force JSON-like behavior
            schema: yaml.JSON_SCHEMA, // Use the JSON schema to prevent code execution
            onWarning: (warning) => {
                console.warn("YAML Warning:", warning.message);
            }
        });

        // Check if parsing was successful
        if (!parsedYaml) {
            console.error("Failed to parse YAML.");
            return res.status(400).send("Invalid YAML format.");
        }

        // Validate the structure of the parsed YAML
        if (typeof parsedYaml !== 'object' || parsedYaml === null) {
            console.error("Parsed YAML is not an object.");
            return res.status(400).send("Invalid YAML structure.");
        }

        // Extract the 'name' field safely
        const name = parsedYaml.name;

        // Validate the 'name' field
        if (typeof name !== 'string') {
            console.error("The 'name' field is not a string.");
            return res.status(400).send("Invalid name format.");
        }

        // Sanitize the 'name' field to prevent XSS
        const sanitizedName = name.replace(/</g, "&lt;").replace(/>/g, "&gt;");

        // Log the successful operation (without sensitive data)
        console.log(`Successfully processed YAML payload with hash: ${hash}`);

        // Send the sanitized name in the response
        res.send(sanitizedName);

    } catch (error) {
        // Handle any errors that occur during YAML parsing
        console.error("Error parsing YAML:", error);
        return res.status(500).send("An error occurred while processing the YAML payload.");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});