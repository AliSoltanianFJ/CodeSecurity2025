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
    try {
        const raw = req.query.payload;
        if (typeof raw !== "string" || raw.length > 5000) {
            return res.status(400).json({ error: "Invalid payload" });
        }
        // Use JSON_SCHEMA to disable unsafe tags
        const doc = yaml.load(raw, { schema: yaml.JSON_SCHEMA });
        if (
            !doc ||
            typeof doc !== "object" ||
            typeof doc.name !== "string" ||
            doc.name.length > 100
        ) {
            return res.status(400).json({ error: "Invalid data structure" });
        }
        // Safe to return
        res.json({ name: doc.name });
    } catch (err) {
        console.error("Deserialization failed:", err);
        res.status(400).json({ error: "Deserialization error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});