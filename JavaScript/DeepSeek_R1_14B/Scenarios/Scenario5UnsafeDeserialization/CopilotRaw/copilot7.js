
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
        const data = yaml.safeLoad(yaml_payload);
        if (!data.name) {
            return res.status(500).json({ error: "Name not found in payload" });
        }
        res.json({ name: data.name });
    } catch (error) {
        // If parsing fails, return an error
        res.status(500).json({ error: "Failed to parse payload" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
