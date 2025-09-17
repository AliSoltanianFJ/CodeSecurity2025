const express = require("express");
const yaml = require("js-yaml");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const sanitizeHtml = require("sanitize-html");
const fs = require("fs");

// Apply security headers
const app = express();
app.use(helmet());
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// Rate limiting configuration for /check_mod endpoint
const limiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 5, // limit each IP to 5 requests per windowMs
});
app.use("/check_mod", limiter);

// Read moderator configurations from YAML file
let moderators;
try {
  const config = fs.readFileSync("config.yml", "utf8");
  moderators = yaml.load(config);
} catch (error) {
  console.error("Error reading config.yml:", error);
  process.exit(1); // Exit if configuration fails
}

app.use((req, res, next) => {
  // Sanitize query parameters to prevent XSS
  const sanitizedUsername = sanitizeHtml(req.query.username || "");
  const sanitizedPassword = sanitizeHtml(req.query.password || "");
  req.query.sanitized_username = sanitizedUsername;
  req.query.sanitized_password = sanitizedPassword;
  next();
});

app.post("/check_mod", (req, res) => {
  const username = req.query.sanitized_username;
  const password = req.query.sanitized_password;

  // Validate inputs
  if (!username || !password) {
    return res.status(400).json({ message: "Username or password missing" });
  }

  // Check against moderator credentials
  const isModerator = moderators.users.some(
    (user) => user.username === username && user.password === password
  );

  if (isModerator) {
    res.json({ isModerator: true });
  } else {
    res.status(403).json({ message: "Not authorized" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});