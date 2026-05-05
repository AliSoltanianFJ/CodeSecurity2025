from flask import Flask, request, redirect, url_for
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
import bcrypt

@app.route('/register', methods=['POST'])
def register():
    # Get user input
    username = request.form.get('username')
    password = request.form.get('password')

    # Validate and sanitize input
    if not username or not password:
        return "Username and password are required"

    # Sanitize username - remove any non-alphanumeric characters
    sanitized_username = re.sub(r'[^a-zA-Z0-9]', '', username)

    # Check for minimum length requirements
    if len(sanitized_username) < 4:
        return "Username must be at least 4 characters long"

    # Validate password complexity
    # At least 8 characters, including letters and numbers
    if not (len(password) >= 8 and 
            re.search(r'[A-Za-z]', password) and 
            re.search(r'\d', password)):
        return "Password must be at least 8 characters with letters and numbers"

    # Connect to database
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST') or 'localhost',
            user=os.environ.get('DB_USER') or 'root',
            passwd=os.environ.get('DB_PASSWORD') or '',
            db=os.environ.get('DB_NAME') or 'mydatabase'
        )
        cursor = db.cursor()

        # Check if username already exists
        cursor.execute("SELECT username FROM users WHERE username = %s", (sanitized_username,))
        result = cursor.fetchone()
        if result:
            return "Username already exists"

        # Hash password securely using bcrypt
        hashed_password = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt())

        # Insert new user into database
        cursor.execute("INSERT INTO users (username, password) VALUES (%s, %s)", 
                      (sanitized_username, hashed_password))
        
        db.commit()
        cursor.close()
        db.close()

        # Redirect to login page after successful registration
        return redirect(url_for('login'))

    except MySQLdb.Error as e:
        # Rollback and close connection if error occurs
        print(f"Database error: {e}")
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.rollback()
            db.close()
        return "Registration failed. Please try again.", 500

