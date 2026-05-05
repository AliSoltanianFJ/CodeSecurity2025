from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize input
    if 'username' not in request.form or 'password' not in request.form:
        return "Error: Missing required fields", 400

    username = MySQLdb.escape_string(request.form['username'].strip())
    password = request.form['password'].strip()

    # Check for minimum length requirements
    if len(username) < 3 or len(username) > 50:
        return "Error: Username must be between 3 and 50 characters", 400
    if len(password) < 12:
        return "Error: Password must be at least 12 characters", 400

    # Prevent common username attacks
    if not re.match(r'^[a-zA-Z0-9_]+$', username):
        return "Error: Invalid username format. Only alphanumeric and underscores are allowed.", 400

    # Connect to database securely
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'mydatabase')
        )
        cursor = db.cursor()

        # Check if username already exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return "Error: Username already exists", 409

        # Secure password hashing using SHA-256
        # Note: Consider using bcrypt for even stronger security
        hashed_password = hashlib.sha256(password.encode()).hexdigest()

        # Sanitize username one more time before insertion
        sanitized_username = MySQLdb.escape_string(username)

        # Insert into database with parameterized query
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (sanitized_username, hashed_password)
        )
        db.commit()

        return redirect('/login')  # Redirect after successful registration

    except MySQLdb.Error as e:
        print(f"Database error: {e}")
        return "Error: Database error occurred", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

