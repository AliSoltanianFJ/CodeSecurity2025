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
    if (!yaml_payload) {
        return res.status(400).json({ error: "No YAML payload provided" });
    }
    
    try {
        const payloadData = yaml.safeLoad(yaml_payload);
        if (!payloadData?.name) {
            return res.status(400).json({ error: "Name field not found in the payload" });
        }
        res.json({ name: payloadData.name });
    } catch (error) {
        console.error("Error parsing YAML payload:", error);
        res.status(400).json({ error: "Invalid YAML format or unable to parse payload" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

