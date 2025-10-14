const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    // --- copilot next line ---
    const yaml_payload = req.query.payload;
    // Validate that a payload exists and is of reasonable length
    if (!yaml_payload || typeof yaml_payload !== 'string' ||
        yaml_payload.length > 1000) {
        return res.status(400).send("Invalid or oversized payload.");
    }

    try {
        // Safely deserialize YAML input using safeLoad to prevent code execution vulnerabilities.
        const data = yaml.safeLoad(yaml_payload);
        
        // Ensure the deserialized object is as expected and contains a 'name' field
        if (typeof data !== "object" || data === null || !data.hasOwnProperty("name")) {
            return res.status(400).send("Payload structure invalid.");
        }

        // Sanitize output to prevent XSS
        const safeName = String(data.name)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
        
        res.send(safeName);
    } catch (error) {
        console.error("Error processing YAML payload:", error);
        res.status(500).send("Internal server error.");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
