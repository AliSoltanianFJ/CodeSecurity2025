const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// 1) Add rate limiting to prevent abuse
const rateLimit = require("express-rate-limit");
const interceptor = require('express-interceptor');
const limiter = rateLimit({
    windowMs: 60 * 1000,
    max: 10,
    message: "Too many requests from this IP, please try again later."
});

// 2) Use a safe YAML schema and validate input more strictly
const safeLoadYaml = (yamlString) => {
    const disallowedChars = /[^a-zA-Z0-9\s\-_:,"'\[\]\{\}\n\r]/;
    if (disallowedChars.test(yamlString)) throw new Error("Invalid characters in YAML input.");
    return yaml.load(yamlString, { schema: yaml.FAILSAFE_SCHEMA });
};

// 3) Validate the 'name' field more strictly (e.g., only allow certain characters)
const validateName = (name) => typeof name === "string" && /^[a-zA-Z\s\-]+$/.test(name);
const nocache = interceptor((req, res) => ({
    isInterceptable: () => true,
    intercept: (body, send) => {
        res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
        res.setHeader('Pragma', 'no-cache');
        res.setHeader('Expires', '0');
        res.setHeader('Surrogate-Control', 'no-store');
        send(body);
    }
}));


// 4) Implement the API endpoint with all the above protections
app.get("/api", limiter, nocache, (req, res) => {
    const allowedKeys = ["payload"];
    Object.keys(req.query).forEach(key => {
        if(!allowedKeys.includes(key)) throw new Error("Unexpected query parameter detected.");
    });
    const yamlPayload = req.query.payload;
    if (typeof yamlPayload !== "string" || yamlPayload.length === 0 || yamlPayload.length > 1024) {
        res.status(400).json({ error: "Invalid payload format." });
        return;
    }
    try {
        const data = safeLoadYaml(yamlPayload);
        if (typeof data !== "object" || data === null) {
            res.status(400).json({ error: "YAML must represent an object." });
            return;
        }
        const keys = Object.keys(data);
        if (!keys.includes("name") || keys.length !== 1) {
            res.status(400).json({ error: "YAML must contain only the 'name' field." });
            return;
        }
        if (!validateName(data.name)) {
            res.status(400).json({ error: "Invalid 'name' value." });
            return;
        }
        res.json({ name: data.name });
    } catch (err) {
        res.status(400).json({ error: "Invalid payload." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
