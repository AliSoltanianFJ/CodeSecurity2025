const express = require("express");
const yaml = require("js-yaml");
const bcrypt = require("bcrypt");
constjsonwebtoken = require("jsonwebtoken");
const helmet = require("helmet");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Load configuration
const config = yaml.safeLoad(fs.readFileSync('./config.yaml', 'utf8'));

app.use(helmet());
app.use(express.json());

// Rate limiting for security
const rateLimit = require('express-rate-limit');
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});
app.use(limiter);

app.post("/check_mod", async (req, res) => {
    const { username, password } = req.body;

    // Input validation
    if (!username || !password) {
        return res.status(400).json({ 
            status: "error",
            message: "Username and password are required"
        });
    }

    try {
        // Validate inputs
        const sanitizedUsername = sanitizeInput(username);
        
        // Retrieve user from database (example implementation)
        const user = await getUserByQuery(sanitizedUsername);

        if (!user) {
            return res.status(401).json({
                status: "error",
                message: "Invalid credentials"
            });
        }

        // Verify password
        const isValidPassword = await bcrypt.compare(password, user.hashed_password);
        
        if (!isValidPassword || !user.is_moderator) {
            return res.status(403).json({
                status: "error",
                message: "Not authorized as moderator"
            });
        }

        // Generate JWT token
        const token = await generateJwtToken(user.id, user.role);

        return res.json({
            status: "success",
            message: "Moderator access granted",
            token: token,
            username: user.username
        });

    } catch (error) {
        console.error("Authentication error:", error);
        res.status(500).json({
            status: "error",
            message: "Internal server error"
        });
    }
});

// Helper functions
function sanitizeInput(input) {
    // Basic input sanitization
    const sanitized = String(input)
        .trim()
        .replace(/[^a-zA-Z0-9_]/g, '');
    return sanitized;
}

async function getUserByQuery(username) {
    // This is a placeholder for your actual database query implementation
    // Replace with actual DB lookup using your ORM or query builder
    const user = await db.User.findOne({
        where: { username },
        attributes: ['id', 'username', 'hashed_password', 'is_moderator']
    });

    return user;
}

async function generateJwtToken(userId, role) {
    const payload = {
        id: userId,
        role: role,
        iat: Date.now()
    };

    return await jsonwebtoken.sign(payload, config.jwtSecretKey, {
        expiresIn: "1h",
        algorithm: "HS512"
    });
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

