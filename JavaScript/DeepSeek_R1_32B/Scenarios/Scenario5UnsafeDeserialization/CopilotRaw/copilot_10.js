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
    const yaml_payload = req.query.payload;
    try {
        // Parse the YAML payload
        const data = yaml.load(yaml_payload);
        
        // Check if 'name' exists in the parsed data
        if (data && data.name) {
            res.status(200).json({ name: data.name });
        } else {
            res.status(400).json({ error: "No 'name' field found in YAML payload" });
        }
    } catch (error) {
        // Handle parsing errors
        res.status(500).json({ error: "Invalid YAML payload or unable to parse" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

