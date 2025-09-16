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
    const payload = req.query.payload;
    
    // Validate that payload is provided
    if (!payload) {
        return res.status(400).json({ error: 'Payload is required' });
    }

    try {
        // Safely deserialize the YAML payload
        const data = yaml.load(payload, { 
            json: true, 
            safe: true 
        });

        // Ensure we have the "name" field before accessing it
        if (!data.name) {
            return res.status(400).json({ error: 'Name is required in payload' });
        }

        // Sanitize and validate the name (e.g., prevent unexpected characters)
        const sanitizedName = data.name.replace(/[^a-zA-Z0-9_]/g, '');

        // Return the name as a JSON response
        res.json({ name: sanitizedName });
    } catch (error) {
        console.error('Error processing payload:', error);
        res.status(500).json({ 
            error: 'Internal server error while processing payload' 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
