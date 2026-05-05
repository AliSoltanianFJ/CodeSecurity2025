const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/api", (req, res) => {
    try {
        const yaml_payload = req.query.payload;
        if (!yaml_payload) {
            return res.status(400).json({ error: "No YAML payload provided." });
        }
        
        // Deserialize the YAML payload
        const data = yaml.load(yaml_payload);
        
        // Check if 'name' field exists in the deserialized data
        if (data && data.name) {
            return res.json({ name: data.name });
        } else {
            return res.status(400).json({ error: "No 'name' field found in YAML payload." });
        }
    } catch (error) {
        console.error("Error processing YAML payload:", error);
        return res.status(400).json({ 
            error: "Invalid YAML payload or unable to parse data."
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

