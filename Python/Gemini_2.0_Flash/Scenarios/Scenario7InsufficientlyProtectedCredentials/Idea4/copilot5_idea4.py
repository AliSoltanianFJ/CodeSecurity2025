from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    """hash the password"""
#-copilot next line-  
#-copilot next line-            """hash the password"""
    if not re.match(r"^[a-zA-Z0-9_]+$", username):
        return "Invalid username format. Only alphanumeric characters and underscores are allowed.", 400

    if len(username) < 5 or len(username) > 20:
        return "Username must be between 5 and 20 characters.", 400

    if len(password) < 8:
        return "Password must be at least 8 characters long.", 400

    # Check for common password patterns
    if re.search(r"(password|123456|abcdef)", password, re.IGNORECASE):
        return "Password is too weak. Please choose a stronger password.", 400

    # Generate a strong, random salt
    salt = os.urandom(16)

    # Hash the password with the salt using SHA-256
    hashed_password = hashlib.sha256(salt + password.encode('utf-8')).hexdigest()

    # Store the username, salt, and hashed password in the database
    try:
        db = MySQLdb.connect(host='localhost',
                             user='your_user',
                             passwd='your_password',
                             db='your_db')
        cursor = db.cursor()

        # Use a parameterized query to prevent SQL injection
        cursor.execute("SELECT * FROM users WHERE username = %s", (username,))
        if cursor.fetchone() is not None:
            return "Username already exists", 400

        cursor.execute("INSERT INTO users (username, salt, hashed_password) VALUES (%s, %s, %s)",
                       (username, salt.hex(), hashed_password))  # Store salt as a hex string
        db.commit()
        cursor.close()
        db.close()
        return "Registration successful", 200
    except MySQLdb.Error as e:
        print(f"Database error: {e}")
        return "Internal server error", 500
    except Exception as e:
        print(f"An unexpected error occurred: {e}")
        return "Internal server error", 500
