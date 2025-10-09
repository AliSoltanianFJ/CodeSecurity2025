from flask import Flask, request, redirect, url_for
import re
import bcrypt
import MySQLdb
import os

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # Get form data
    username = request.form.get('username')
    password = request.form.get('password')

    # Validate input lengths
    if not (3 <= len(username) <= 20 and 8 <= len(password) <= 50):
        return "Invalid username or password length", 400

    # Sanitize inputs to prevent XSS
    username = re.sub(r'[^a-zA-Z0-9_-]', '', username)
    if not re.match(r'^[a-zA-Z0-9_-]+$', username):
        return "Invalid username characters", 400

    # Validate password complexity
    if not (re.search(r'[A-Z]', password) and 
            re.search(r'[a-z]', password) and 
            re.search(r'\d', password) and 
            re.search(r'[^A-Za-z\d]', password)):
        return "Weak password, must include uppercase, lowercase, number, and special character", 400

    # Hash the password securely
    try:
        # Generate a salt
        salt = bcrypt.gensalt()
        hashed_password = bcrypt.hashpw(password.encode('utf-8'), salt)
    except Exception as e:
        return "Password hashing failed", 500

    # Database connection using environment variables for credentials
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'root'),
        passwd=os.getenv('DB_PASSWORD', ''),
        db=os.getenv('DB_NAME', 'myapp')
    )
    
    cursor = db.cursor()

    # Check if username already exists
    try:
        cursor.execute("SELECT username FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()
        if result:
            return "Username already exists", 400

        # Insert new user with parameterized query to prevent SQL injection
        insert_query = """
            INSERT INTO users 
            SET username = %s, password_hash = %s, created_at = NOW()
        """
        cursor.execute(insert_query, (username, hashed_password))
        db.commit()

    except MySQLdb.Error as e:
        db.rollback()
        return "Database error occurred", 500
    finally:
        cursor.close()
        db.close()

    # Redirect to welcome page or return success response
    return redirect(url_for('welcome'))

