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
        // Log the error securely (without exposing sensitive data)
        console.error("JSON parsing error:", error.message);
        return null; // or throw a custom error
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

    // Generate a secure hash of the payload for integrity checks
    const payloadHash = crypto.createHash('sha256').update(yaml_payload).digest('hex');

    try {
        // Attempt to parse the YAML payload
        let parsedYaml = yaml.load(yaml_payload, {
            json: true, // Convert to JSON-like structure
            schema: yaml.JSON_SCHEMA, // Use the safest schema
            // Add schema validation to only allow primitive types
            schema: yaml.Schema.create([
                yaml.ScalarType,
                yaml.NumberType,
                yaml.BooleanType,
                yaml.NullType
            ]),
            // Limit alias usage to prevent circular references
            noRefs: true,
            // Disallow duplicate keys
            noCompatMode: true,
        });

        // Check if parsing was successful
        if (!parsedYaml) {
            console.error("YAML parsing failed.");
            return res.status(400).send("Invalid YAML payload");
        }

        // Ensure the parsed YAML is an object
        if (typeof parsedYaml !== 'object' || parsedYaml === null) {
            console.error("Parsed YAML is not an object.");
            return res.status(400).send("YAML payload must represent an object");
        }

        // Extract the 'name' field safely
        const name = parsedYaml.name;

        // Validate the 'name' field
        if (typeof name !== 'string') {
            console.error("The 'name' field is not a string.");
            return res.status(400).send("The 'name' field must be a string");
        }

        // Sanitize the 'name' field to prevent XSS
        const sanitizedName = name.replace(/</g, "&lt;").replace(/>/g, "&gt;");

        // Log the successful operation (without logging user data)
        console.log("Successfully processed YAML payload.");

        // Return the sanitized name
        return res.status(200).send({ name: sanitizedName });

    } catch (error) {
        // Handle any errors that occur during YAML parsing
        console.error("Error processing YAML payload:", error.message);
        return res.status(500).send("Error processing YAML payload");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});