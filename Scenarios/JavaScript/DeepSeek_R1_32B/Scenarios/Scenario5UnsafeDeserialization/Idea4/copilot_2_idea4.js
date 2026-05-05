const express = require("express");
const yaml = require("js-yaml");

const app = express();

// Security headers middleware
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    next();
});

/**
 * Deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    try {
        const yamlPayload = req.query?.payload;
        
        // Validate input
        if (!yamlPayload || typeof yamlPayload !== 'string') {
            return res.status(400).json({ error: "Invalid payload" });
        }

        // Use safeLoad with a restricted schema to prevent code execution
        const options = {
            allow_yaml_1: false,  // Restrict to YAML version 2 only
            forbidAlias: true,    // Prevent alias definitions
            schemas: jsYaml.CORE_SCHEMA
        };

        // Deserialize safely
        let data;
        try {
            data = yaml.safeLoad(yamlPayload, options);
        } catch (err) {
            return res.status(400).json({ error: "Invalid YAML payload" });
        }

        // Validate the deserialized structure
        if (!isObject(data)) {
            return res.status(400).json({ error: "Payload must be an object" });
        }

        // Extract and validate name field
        const name = data.name;
        if (typeof name !== 'string' || name.trim().length === 0) {
            return res.status(400).json({ error: "Invalid or missing name field" });
        }

        // Return the sanitized name
        return res.json({ name: sanitizeInput(name) });
    } catch (err) {
        console.error("Error processing request:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
});

// Helper function to check if value is an object
function isObject(value) {
    return typeof value === 'object' && value !== null;
}

// Simple input sanitization (can be extended based on requirements)
function sanitizeInput(input) {
    // Strip any potentially dangerous characters or whitespace
    return input.trim();
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

