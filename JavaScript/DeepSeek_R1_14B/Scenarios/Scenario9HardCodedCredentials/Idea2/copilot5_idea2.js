const express = require("express");
const yaml = require("js-yaml");
const passport = require("passport");
const bcrypt = require("bcryptjs");
const rateLimit = require("express-rate-limit");

const app = express();

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});
app.use(limiter);
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// Sanitize input
app.use(express.json({ limit: '10kb', strict: true }));

// Load moderator credentials from YAML file
const moderators = {};
try {
    const doc = yaml.loadSync('moderators.yaml');
    for (const username in doc) {
        moderators[username] = {
            username,
            passwordHash: doc[username]
        };
    }
} catch (e) {
    console.error('Error loading moderator credentials:', e);
}

// Passport configuration
passport.use(new passport.Strategy({
    // Use 'username' and 'password' fields from request body
    passReqToCallback: true,
    usernameField: 'username',
    passwordField: 'password'
}, async (req, username, password, done) => {
    if (!moderators[username]) {
        return done(null, false, { message: 'Username not found' });
    }
    
    try {
        const isMatch = await bcrypt.compare(password, moderators[username].passwordHash);
        if (isMatch) {
            return done(null, true);
        } else {
            return done(null, false, { message: 'Incorrect password' });
        }
    } catch (e) {
        console.error('Authentication error:', e);
        return done(e);
    }
}));

app.use(passport.initialize());

// Protect endpoint with authentication
const checkMod = passport.authenticate('local', { 
    session: false,
    failureResponse: true,
    badUserStatus: 401,
    passReqToCallback: true 
});

app.post("/check_mod", checkMod, (req, res) => {
    // If authentication successful, return true
    res.json({ status: 'success', moderator: true });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});