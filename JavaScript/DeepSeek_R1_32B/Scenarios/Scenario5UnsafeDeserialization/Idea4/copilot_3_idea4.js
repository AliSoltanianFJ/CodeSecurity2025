const express = require("express");
const yaml = require("js-yaml");
const Ajv = require("ajv"); // For JSON schema validation
const ajv = new Ajv();

const app = express();

// Security headers middleware
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Type', 'application/json');
    next();
});

/**
 * Securely deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    // Input validation
    if (!yaml_payload || typeof yaml_payload !== 'string') {
        return res.status(400).json({ error: "Invalid or missing YAML payload" });
    }

    try {
        // Maximum allowed size for the YAML payload to prevent resource exhaustion
        const MAX_PAYLOAD_SIZE = 1024 * 100; // 100KB limit
        if (yaml_payload.length > MAX_PAYLOAD_SIZE) {
            return res.status(400).json({ error: "Payload too large" });
        }

        // Safely deserialize the YAML payload using js-yaml's safeLoad method
        const data = yaml.safeLoad(yaml_payload);

        // Define JSON schema for validation
        const nameSchema = ajv.compile({
            type: 'object',
            required: ['name'],
            properties: {
                name: { 
                    type: 'string',
                    minLength: 1,
                    maxLength: 255
                }
            },
            additionalProperties: false // Rejects any extra fields
        });

        // Validate the deserialized data against the schema
        if (!nameSchema(data)) {
            return res.status(400).json({ 
                error: "Invalid payload format",
                details: nameSchema.errors?.map(e => e.message) || []
            });
        }

        // Sanitize and return the 'name' field
        const safeName = data.name.trim();
        
        res.json({ name: safeName });

    } catch (error) {
        console.error("Error processing YAML payload:", error);
        res.status(500).json({ 
            error: "Internal server error while processing payload",
            details: error.message
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

