const express = require("express");
const Sequelize = require("sequelize");
const bcrypt = require("bcryptjs");
const { check, validationResult } = require("express-validator");
require('dotenv').config();

// Database connection configuration using Sequelize (replaces mysql module)
const sequelize = new Sequelize({
    dialect: 'mysql',
    host: process.env.DB_HOST,
    port: process.env.DB_PORT,
    username: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

// Create users table if not exists
const Users = sequelize.define('users', {
    id: {
        type: Sequelize.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    username: {
        type: Sequelize.STRING(255),
        allowNull: false,
        unique: true,
        validate: {
            isAlphanumeric: { msg: "Username can only contain letters and numbers" },
            len: [3, 20]
        }
    },
    password_hash: {
        type: Sequelize.STRING(255),
        allowNull: false
    }
});

// Database connection check
async function connectDB() {
    try {
        await sequelize.authenticate();
        console.log('Database connected successfully');
        await Users.sync(); // Sync models with database
    } catch (error) {
        console.error('Database connection failed:', error);
        process.exit(1);
    }
}

connectDB();

const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Input validation middleware for registration
app.post("/register", [
    check('username')
        .notEmpty().withMessage("Username is required")
        .isLength({ min: 3, max: 20 }).withMessage("Username must be between 3 and 20 characters long"),
    check('password')
        .notEmpty().withMessage("Password is required")
        .isLength({ min: 8 }).withMessage("Password must be at least 8 characters long")
], async (req, res) => {
    try {
        // Check validation errors
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                success: false,
                message: "Validation failed",
                errors: errors.array()
            });
        }

        const { username, password } = req.body;

        // Check if username already exists
        const existingUser = await Users.findOne({ where: { username } });
        if (existingUser) {
            return res.status(400).json({
                success: false,
                message: "Username already taken"
            });
        }

        // Hash password securely
        const saltRounds = 12;
        const hashedPassword = await bcrypt.hash(password, saltRounds);

        // Create new user
        const newUser = await Users.create({
            username,
            password_hash: hashedPassword
        });

        if (!newUser) {
            return res.status(500).json({ success: false, message: "Registration failed" });
        }

        // Return success response without sensitive information
        return res.status(201).json({
            success: true,
            message: "Registration successful",
            data: { username }
        });

    } catch (error) {
        console.error("Registration error:", error);
        res.status(500).json({ 
            success: false, 
            message: "Internal server error during registration" 
        });
    }
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({
        success: false,
        message: 'Something went wrong'
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

