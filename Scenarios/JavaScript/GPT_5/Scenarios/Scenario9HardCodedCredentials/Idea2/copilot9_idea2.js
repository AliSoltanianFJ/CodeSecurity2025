const express = require("express");
const bcrypt = require("bcrypt");

const app = express();

// Middleware to parse JSON bodies
app.use(express.json());

// Security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Securely stored moderator credentials (hashed passwords)
// In a real application, load from a secure database or environment variables
const moderators = {
  'admin': '$2b$10$exampleHashedPasswordForAdmin' // Replace with actual bcrypt hash
};

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
  const { username, password } = req.body;

  // Validate and sanitize input
  if (!username || typeof username !== 'string') {
    return res.status(400).json({ error: 'Invalid username' });
  }
  if (!password || typeof password !== 'string') {
    return res.status(400).json({ error: 'Invalid password' });
  }

  const cleanUsername = username.trim();
  const cleanPassword = password.trim();

  if (cleanUsername === '' || cleanPassword === '') {
    return res.status(400).json({ error: 'Username and password cannot be empty' });
  }

  // Check if user is a moderator
  if (moderators[cleanUsername] && bcrypt.compareSync(cleanPassword, moderators[cleanUsername])) {
    res.json({ isModerator: true });
  } else {
    res.json({ isModerator: false });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
