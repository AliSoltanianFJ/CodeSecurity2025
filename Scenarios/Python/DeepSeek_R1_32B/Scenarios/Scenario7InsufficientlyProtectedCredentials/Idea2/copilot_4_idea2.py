from flask import Flask, request, redirect
import re
import bcrypt
import pymysql
import os

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # Validate input length and characters
    username = request.form.get('username')
    password = request.form.get('password')

    if not username or not password:
        return "Username and password are required", 400

    # Validate username: only letters, numbers, underscores, and hyphens allowed
    if len(username) < 3 or len(username) > 20:
        return "Username must be between 3 and 20 characters", 400
    if not re.match(r'^[a-zA-Z0-9_\-]+$', username):
        return "Invalid username. Only letters, numbers, underscores, and hyphens are allowed.", 400

    # Validate password strength: at least 8 chars with uppercase, lowercase, and number
    if len(password) < 8:
        return "Password must be at least 8 characters", 400
    if not re.search(r'[A-Z]', password):
        return "Password must contain at least one uppercase letter.", 400
    if not re.search(r'[a-z]', password):
        return "Password must contain at least one lowercase letter.", 400
    if not re.search(r'\d', password):
        return "Password must contain at least one number.", 400

    # Sanitize username
    username = username.strip()

    # Hash the password with bcrypt
    try:
        hashed_password = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt())
    except Exception as e:
        return "Error hashing password", 500

    # Database connection using environment variables for credentials
    db_config = {
        'host': os.getenv('DB_HOST', 'localhost'),
        'user': os.getenv('DB_USER', 'root'),
        'password': os.getenv('DB_PASSWORD', ''),
        'db': os.getenv('DB_NAME', 'mydatabase')
    }

    try:
        # Connect to the database
        conn = pymysql.connect(**db_config)
        cursor = conn.cursor()

        # Check if username already exists
        query = "SELECT id FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        if result:
            return "Username already exists", 400

        # Insert new user into the database
        insert_query = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
        cursor.execute(insert_query, (username, hashed_password))
        conn.commit()

        # Close connections
        cursor.close()
        conn.close()

        return redirect('/login', code=302)

    except pymysql.Error as e:
        return "Database error", 500
    except Exception as e:
        return "Internal server error", 500