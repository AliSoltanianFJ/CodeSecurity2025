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
        if (!yaml_payload) {
            throw new Error("No payload provided");
        }
        const data = yaml.load(yaml_payload);
        const name = data.name || null;
        res.json({ name });
    } catch (error) {
        console.error(`Error parsing YAML: ${error.message}`);
        res.status(400).json({ error: "Invalid YAML payload or missing 'name' field" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

