from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Get form data
    username = request.form.get('username')
    password = request.form.get('password')

    if not username or not password:
        return "Username and password are required", 400

    # Validate inputs
    if len(username) < 3 or len(username) > 20:
        return "Username must be between 3 and 20 characters", 400
    
    if len(password) < 8 or len(password) > 50:
        return "Password must be between 8 and 50 characters", 400

    # Sanitize inputs
    username = re.escape(username)
    
    # Generate salt and hash password using sha256
    salt = os.urandom(16).hex()
    hashed_password = hashlib.sha256(salt.encode() + password.encode()).hexdigest()

    try:
        # Connect to MySQL database
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASSWD', ''),
            db=os.getenv('DB_NAME', 'mydatabase')
        )
        
        cursor = db.cursor()

        # Check if username already exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return "Username already exists", 409

        # Insert new user with hashed password and salt
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_password, salt)
        )
        
        db.commit()
        cursor.close()
        db.close()

        # Redirect to login page or dashboard
        return redirect('/login')

    except MySQLdb.Error as e:
        print(f"Database error")
        return "Internal server error", 500
    except Exception as e:
        print(f"Unexpected error")
        return "Internal server error", 500

