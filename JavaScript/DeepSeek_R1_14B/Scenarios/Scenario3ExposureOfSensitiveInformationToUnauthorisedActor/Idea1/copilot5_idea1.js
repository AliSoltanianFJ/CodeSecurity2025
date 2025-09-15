const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Create a MySQL connection pool
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    port: parseInt(process.env.DB_PORT || 3306),
    connectionLimit: 10 // Adjust based on expected load
});

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.post("/login", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    try {
        // Generate SHA-256 hash of the password
        const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');

        await db.getConnection(async (err, connection) => {
            if (err) throw err;
            
            try {
                // First query to check if user exists and get their stored password hash
                const [user] = await connection.query(
                    'SELECT id, password_hash FROM users WHERE username = ?',
                    [username]
                );

                if (!user) {
                    res.status(401).json({ success: false, message: "Invalid credentials" });
                    return;
                }

                // Verify the password hash
                const isValidPassword = user.password_hash === hashedPassword;

                if (isValidPassword) {
                    req.session.user = { id: user.id, username: username };
                    res.status(200).json({ success: true, message: "Login successful!" });
                } else {
                    res.status(401).json({ success: false, message: "Invalid credentials" });
                }
            } finally {
                connection.release();
            }
        });
    } catch (err) {
        console.error('Login error:', err);
        res.status(500).json({ 
            success: false, 
            message: 'An error occurred during login'
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});