const express = require("express");
const yaml = require("js-yaml");
const Joi = require("joi");
const rateLimit = require("express-rate-limit");

const app = express();

// Rate limiting to prevent brute force attacks
const limiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 5 // limit each IP to 5 requests per minute
});

app.use(limiter);

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Read configuration file
function getConfig() {
    const config = {};
    try {
        const file = require("fs").readFileSync("config.yaml", "utf8");
        config = yaml.load(file);
    } catch (e) {
        console.error("Failed to read config file:", e.message);
    }
    return config;
}

const CONFIG = getConfig();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Validate and sanitize input
function validateUser(data) {
    const schema = Joi.object({
        username: Joi.string().required(),
        password: Joi.string().required()
    });
    
    return schema.validate(data);
}

app.post("/check_mod", (req, res) => {
    // Read credentials from config file instead of hardcoding
    const { moderatorUsername, moderatorPassword } = CONFIG;
    
    try {
        // Validate input
        const validation = validateUser(req.body);
        
        if (validation.error) {
            return res.status(400).send("Invalid request format");
        }

        // Sanitize username to prevent XSS
        const sanitizedUsername = require('sanitize-html')(req.body.username);

        // Basic authentication check - in real scenario, use proper password hashing
        if (sanitizedUsername === moderatorUsername && req.body.password === moderatorPassword) {
            return res.status(200).json({ isModerator: true });
        } else {
            return res.status(401).json({ isModerator: false, message: "Invalid credentials" });
        }
    } catch (e) {
        console.error("Error in check_mod:", e);
        res.status(500).send("Internal server error");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});