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
        return "Invalid username format. Only alphanumeric characters and underscores are allowed."

    if len(username) < 5 or len(username) > 20:
        return "Username must be between 5 and 20 characters."

    if len(password) < 8:
        return "Password must be at least 8 characters."

    if not re.search(r"[a-z]", password):
        return "Password must contain at least one lowercase letter."

    if not re.search(r"[A-Z]", password):
        return "Password must contain at least one uppercase letter."

    if not re.search(r"[0-9]", password):
        return "Password must contain at least one number."

    if not re.search(r"[^a-zA-Z0-9]", password):
        return "Password must contain at least one special character."
    
    hashed_password = hashlib.pbkdf2_hmac(
        'sha512',  # The hash digest algorithm for HMAC.
        password.encode('utf-8'),  # Convert the password to bytes
        os.urandom(16),  # Provide a salt
        100000  # It is recommended to use at least 100000 iterations of SHA-256
    )

    """store the username and hashed password in the database"""
    try:
        db = MySQLdb.connect(host='localhost',
                             user=os.environ.get('DB_USER'),
                             passwd=os.environ.get('DB_PASSWORD'),
                             db='mydatabase')
        cursor = db.cursor()
        cursor.execute("SELECT * FROM users WHERE username = %s", (username,))
        if cursor.fetchone() is not None:
            return "Username already exists"
        cursor = db.cursor()
        cursor.execute("INSERT INTO users (username, password) VALUES (%s, %s)", (username, hashed_password))
        db.commit()
    except MySQLdb.Error as e:
        print(f"Database error: {e}")
        return "Registration failed due to a database error."
    finally:
        if db:
            db.close()
    return "Registration successful"
