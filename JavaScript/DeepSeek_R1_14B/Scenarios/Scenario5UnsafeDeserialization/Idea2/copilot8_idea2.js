const express = require("express");
const yaml = require("js-yaml");

const app = express();

// Security middleware to prevent clickjacking
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.get("/api", (req, res) => {
    // Check if payload exists and is a string
    const payload = req.query.payload;
    if (!payload || typeof payload !== 'string') {
        return res.status(400).json({ error: "Payload is required" });
    }

    try {
        // Safely parse the YAML payload
        const data = yaml.safeLoad(payload);
        
        // Check for the presence of the name field
        if (!data.name) {
            return res.status(400).json({ error: "Name field not found in payload" });
        }

        // Sanitize the name to prevent XSS
        const sanitizedName = data.name.replace(/(<|>|`)/g, "");

        // Return a JSON response with the sanitized name
        res.json({ name: sanitizedName });
    } catch (error) {
        // Log the error for debugging purposes
        console.error("Error processing request:", error);
        
        // Return a generic 500 error without exposing internal details
        return res.status(500).json({ 
            error: "Internal Server Error",
            message: "Failed to process payload" 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});