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
        return null; // or throw an error, depending on your error handling policy
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
    
    // Validate payload length to prevent excessively large payloads
    if (yaml_payload.length > 2048) {
        return res.status(400).send("Payload too large");
    }

    // Generate a secure hash of the payload for logging purposes.  Do NOT log the payload directly.
    const payloadHash = crypto.createHash('sha256').update(yaml_payload).digest('hex');

    try {
        // Options to disallow arbitrary code execution
        const safeLoadOptions = {
            json: true,
            schema: yaml.JSON_SCHEMA, // Use the JSON schema to prevent code execution
            // Add more options as needed to restrict functionality
        };

        // Securely load the YAML payload
        const parsedYaml = yaml.load(yaml_payload, safeLoadOptions);

        // Check if parsedYaml is null or undefined
        if (!parsedYaml) {
            console.warn(`YAML payload parsed to null or undefined. Payload hash: ${payloadHash}`);
            return res.status(400).send("Invalid YAML payload");
        }

        // Check if parsedYaml is an object
        if (typeof parsedYaml !== 'object') {
            console.warn(`YAML payload did not parse to an object. Payload hash: ${payloadHash}`);
            return res.status(400).send("YAML payload must represent an object");
        }

        // Extract the name field
        let name = parsedYaml.name;

        // Validate the extracted name
        if (typeof name !== 'string') {
            console.warn(`Extracted 'name' is not a string. Payload hash: ${payloadHash}`);
            return res.status(400).send("'name' field must be a string");
        }

        // Sanitize the name field to prevent XSS
        name = name.replace(/</g, "&lt;").replace(/>/g, "&gt;");

        // Log the successful extraction (without logging the name directly)
        console.log(`Successfully extracted 'name' from YAML payload. Payload hash: ${payloadHash}`);

        // Send the sanitized name in the response
        res.send({ name: name });

    } catch (error) {
        // Log the error (without including the payload)
        console.error(`Error processing YAML payload. Payload hash: ${payloadHash}`, error);
        return res.status(500).send("An error occurred while processing the YAML payload");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});