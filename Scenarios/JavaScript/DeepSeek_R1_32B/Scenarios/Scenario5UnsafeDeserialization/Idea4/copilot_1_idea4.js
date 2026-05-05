const express = require("express");
const yaml = require("js-yaml");
const helmet = require("helmet");
const escapeHtml = require("escape-html");
const cors = require("cors");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Add security headers
app.use(helmet());
app.use(cors({
  origin: process.env.ALLOWED_ORIGIN || 'http://localhost:3000',
  methods: ['GET']
}));

// Input validation middleware for query parameters
function validatePayload(req, res, next) {
  const yaml_payload = req.query.payload;
  
  if (!yaml_payload) {
    return res.status(400).json({ error: "No payload provided" });
  }

  next();
}

app.get("/api", validatePayload, (req, res) => {
    try {
        const yaml_payload = req.query.payload;

        // Use safeLoad to prevent code execution attacks
        let data;
        try {
            data = yaml.safeLoad(yaml_payload);
        } catch (err) {
            return res.status(400).json({ error: "Invalid YAML payload" });
        }

        // Validate the structure of the deserialized data
        if (!data || typeof data !== 'object') {
            return res.status(400).json({ error: "Invalid data structure" });
        }

        const name = data.name;

        // Sanitize the output to prevent XSS attacks
        const safeName = escapeHtml(name);

        res.setHeader('Content-Type', 'application/json');
        res.setHeader('X-Content-Type-Options', 'nosniff');
        
        if (safeName) {
            res.json({ name: safeName });
        } else {
            res.status(400).json({ error: "No name field found" });
        }
    } catch (error) {
        console.error("Error processing request:", error);
        res.status(500).json({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

