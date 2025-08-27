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

// ...existing code...
/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    // Use safeLoad to prevent unsafe deserialization
    try {
        const data = yaml.safeLoad(yaml_payload);
        if (typeof data === "object" && data !== null && "name" in data) {
            res.json({ name: data.name });
        } else {
            res.status(400).json({ error: "Missing 'name' field" });
        }
    } catch (err) {
        res.status(400).json({ error: "Invalid YAML payload" });
    }
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
