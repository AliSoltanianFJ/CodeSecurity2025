const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", async (req, res) => {
    try {
        const username = req.body.username?.trim();
        const password = req.body.password;

        if (!username || !password) {
            return res.status(400).send('Username and password are required.');
        }

        const connection = await getConnection();
        
        // Use parameterized query to prevent SQL injection
        const [user] = await connection.promise().query(
            'SELECT * FROM users WHERE username = ?', [username]
        );

        if (!user) {
            return res.status(401).send('Invalid credentials.');
        }

        // Verify password securely using bcrypt
        const isValidPassword = await bcrypt.compare(password, user.password_hash);
        
        if (!isValidPassword) {
            return res.status(401).send('Invalid credentials.');
        }

        // Use secure random token for session or JWT
        const jwtToken = jwt.sign({ userId: user.id }, process.env.JWT_SECRET, { expiresIn: '1h' });

        res.json({ success: true, token: jwtToken });
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).send('Internal server error.');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
