const express = require("express");
const yaml = require("js-yaml");
const rateLimit = require("express-rate-limit");

const app = express();

// Rate limiting configuration
const limiter = rateLimit({
  windowMs: 3600000, // 1 hour in milliseconds
  max: 100 // limit each IP to 100 requests per hour
});

app.use(limiter);

// Read moderator data from YAML file
let moderators;
try {
    const filePath = __dirname + "/moderators.yaml";
    const fileContent = fs.readFileSync(filePath, 'utf8');
    moderators = yaml.safeLoad(fileContent).moderators || [];
} catch (error) {
    console.error("Error loading moderators:", error);
    process.exit(1); // Exit if cannot load moderator data
}

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.post("/check_mod", (req, res) => {
    const username = req.query.username?.trim().escape() || '';
    const password = req.query.password?.trim().escape() || '';

    if (!username || !password) {
        return res.status(400).json({ message: "Username and password are required" });
    }

    // Check each moderator for matching credentials
    const isModerator = moderators.some(moderator => 
        moderator.username === username && moderator.password === password
    );

    if (isModerator) {
        res.status(200).json({ status: true });
    } else {
        res.status(403).json({ status: false, message: "Invalid credentials" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});