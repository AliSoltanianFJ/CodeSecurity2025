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

// Securely deserialize YAML with schema validation and input sanitization
function safeYamlLoad(yaml_payload) {
    if (typeof yaml_payload !== 'string') {
        console.error("Invalid payload type. Expected string.");
        return null;
    }

    // Input sanitization: Limit payload size to prevent DoS attacks
    if (yaml_payload.length > 1024) {
        console.error("Payload too large.");
        return null;
    }

    // Input validation: Check for potentially malicious YAML constructs using regex
    const maliciousPatterns = [
        /!!js\/function/,  // JavaScript function execution
        /javascript:/i,    // JavaScript execution
        /expression:/i,    // Expression evaluation
        /<\s*script/i,     // HTML script tags
        /<iframe>/i,         // HTML iframes
        /file:\/\//i,        // File access
        /ftp:\/\//i,         // FTP access
        /http:\/\//i         // HTTP access (SSRF risk)
    ];

    for (const pattern of maliciousPatterns) {
        if (pattern.test(yaml_payload)) {
            console.error("Payload contains potentially malicious content.");
            return null;
        }
    }

    try {
        // Use safe schema to prevent code execution
        const data = yaml.load(yaml_payload, {
            schema: yaml.SAFE_SCHEMA,
            json: true
        });

        // Validate the structure of the deserialized data
        if (!data || typeof data !== 'object' || !data.name || typeof data.name !== 'string') {
            console.error("Invalid data structure in YAML payload.");
            return null;
        }

        // Sanitize the 'name' field to prevent XSS
        const sanitizedName = sanitizeString(data.name);
        return { name: sanitizedName };

    } catch (error) {
        // Log the error (without including the payload)
        console.error("Error deserializing YAML:", error);
        return null;
    }
}

// Secure string sanitization function to prevent XSS
function sanitizeString(str) {
    if (typeof str !== 'string') {
        return '';
    }
    // Escape HTML entities to prevent XSS
    return str.replace(/&/g, '&amp;')
              .replace(/</g, '&lt;')
              .replace(/>/g, '&gt;')
              .replace(/"/g, '&quot;')
              .replace(/'/g, '&#39;')
              .replace(/\//g, '&#x2F;');
}

/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    if (!yaml_payload) {
        return res.status(400).send("Payload is required");
    }

    const parsedData = safeYamlLoad(yaml_payload);

    if (!parsedData) {
        return res.status(400).send("Invalid payload");
    }

    res.setHeader('Content-Type', 'text/plain');
    res.send(parsedData.name);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});