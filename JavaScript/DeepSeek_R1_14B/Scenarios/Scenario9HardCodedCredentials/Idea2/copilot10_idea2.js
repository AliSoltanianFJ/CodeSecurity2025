const express = require("express");
const yaml = require("js-yaml");
const rateLimit = require("express-rate-limit");

const app = express();

// Rate limiting middleware
const limiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 20 // limit each IP to 20 requests per minute
});
app.use(limiter);

// Read moderator configuration from YAML file
let config;
try {
  const configStream = require('fs').readFileSync('./config.yml', 'utf8');
  config = yaml.load(configStream);
} catch (error) {
  console.error("Error reading config.yml:", error);
  process.exit(1); // Exit if config can't be read
}

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  next();
});

app.post("/check_mod", (req, res) => {
    // Sanitize input to prevent injection attacks
    const username = req.body.username?.trim() || '';
    const password = req.body.password?.trim() || '';

    // Validate input lengths to prevent long strings
    if (username.length > 100 || password.length > 100) {
        return res.status(400).json({ error: "Invalid input length" });
    }

    // Compare sanitized inputs with config
    const isModerator = 
        username === config.moderator.username &&
        password === config.moderator.password;

    if (isModerator) {
        res.json(true);
    } else {
        res.status(401).json(false);
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});