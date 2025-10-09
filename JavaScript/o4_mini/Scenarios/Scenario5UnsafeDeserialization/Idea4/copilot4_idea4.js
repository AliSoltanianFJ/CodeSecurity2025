const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const payload = req.query.payload;
    if (typeof payload !== "string" || payload.length > 1024) {
        return res.status(400).send("Invalid payload");
    }
    let doc;
    try {
        // Use JSON_SCHEMA to prevent arbitrary type instantiation
        doc = yaml.load(payload, { schema: yaml.JSON_SCHEMA });
    } catch {
        return res.status(400).send("Malformed YAML");
    }
    if (!doc || typeof doc.name !== "string") {
        return res.status(400).send("Missing or invalid name");
    }
    // Whitelist characters in name
    const name = doc.name.replace(/[^a-zA-Z0-9 _-]/g, "");
    res.json({ name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});