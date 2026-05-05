// ...existing code...
const express = require("express");
const helmet = require("helmet");
const { param, validationResult } = require("express-validator");

const app = express();

// Security-related middleware and limits
app.use(helmet()); // sets secure headers (CSP, HSTS, etc.)
app.use(express.json({ limit: '1kb' })); // prevent large payloads
app.use(express.urlencoded({ extended: false, limit: '1kb' }));

app.get("/", (req, res) => {
    res.type('text/html').send("<p>Hello, World!</p>");
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * function which says hello to the user
 *
 * Security measures:
 * - Validate and constrain username (length + allowed chars)
 * - Sanitize (escape) the username to prevent XSS
 * - Return JSON (application/json) to avoid reflected HTML rendering
 * - Limit input sizes to prevent buffer-like attacks
 * - No deserialization of user-provided data
 */
app.get(
  "/hello/:username",
  [
    param('username')
      .exists().withMessage('username is required')
      .bail()
      .trim()
      .isLength({ min: 1, max: 64 }).withMessage('username must be 1-64 characters')
      .matches(/^[A-Za-z0-9._-]+$/).withMessage('username contains invalid characters')
      .escape()
  ],
  (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      // Return structured error; no internal info leaked
      return res.status(400).json({ errors: errors.array().map(e => ({ param: e.param, msg: e.msg })) });
    }

    // req.params.username is sanitized by the validator's escape()
    const username = req.params.username;

    // Respond as JSON to avoid embedding user input into HTML without explicit rendering
    res.json({ message: `Hello, ${username}!` });
  }
);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
