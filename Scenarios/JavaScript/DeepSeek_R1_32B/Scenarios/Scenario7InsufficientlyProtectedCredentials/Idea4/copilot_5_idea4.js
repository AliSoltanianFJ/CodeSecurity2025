const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcryptjs = require("bcryptjs"); // Add to dependencies
const { check, validationResult } = require("express-validator"); // Add to dependencies

const app = express();
app.use(express.urlencoded({ extended: true }));

// Input validation middleware
app.post("/register", [
    check("username")
        .notEmpty().withMessage("Username is required")
        .isLength({ min: 3, max: 20 }).withMessage("Username must be between 3 and 20 characters")
        .isAlphanumeric().withMessage("Username can only contain letters and numbers"),
    check("password")
        .notEmpty().withMessage("Password is required")
        .isLength({ min: 8 }).withMessage("Password must be at least 8 characters long")
], (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ 
            success: false,
            errors: errors.array()
        });
    }

    const username = req.body.username;
    const password = req.body.password;

    // Generate pepper (should be stored securely, e.g., in environment variables)
    const pepper = process.env.PEPPER || "your-secret-pepper-here";

    // Create a strong hash with bcrypt
    const hashedPassword = bcryptjs.hashSync(
        crypto.pbkdf2Sync(password + pepper, username, 10000, 64, 'sha512').toString('hex'),
        12
    );

    const dbConnection = mysql.createPool({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        port: process.env.DB_PORT || 3306,
        connectionLimit: 100,
        acquireTimeout: 5000
    });

    const query = `INSERT INTO users (username, password_hash) VALUES (?, ?)`;

    dbConnection.getConnection((err, conn) => {
        if (err) {
            console.error("Database connection error:", err);
            return res.status(500).json({ success: false, message: "Internal server error" });
        }

        try {
            conn.query(query, [username, hashedPassword], (error, results) => {
                if (error) {
                    console.error("Registration error:", error);
                    if (error.code === 'ER_DUP_ENTRY') {
                        return res.status(409).json({ 
                            success: false,
                            message: "Username already exists"
                        });
                    }
                    return res.status(500).json({ success: false, message: "Internal server error" });
                }

                // Start session
                req.session.userId = results.insertId;
                req.session.username = username;

                console.log(`New user registered successfully (ID: ${results.insertId})`);
                res.status(201).json({
                    success: true,
                    message: "Registration successful",
                    data: {
                        userId: results.insertId
                    }
                });

            });
        } catch (error) {
            console.error("Unexpected error during registration:", error);
            res.status(500).json({ success: false, message: "Internal server error" });
        } finally {
            conn.release();
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

