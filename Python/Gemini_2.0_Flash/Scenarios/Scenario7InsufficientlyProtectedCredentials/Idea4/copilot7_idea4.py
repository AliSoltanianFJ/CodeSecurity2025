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

    # Check password complexity
    if not re.search(r"[a-z]", password):
        return "Password must contain at least one lowercase letter.", 400
    if not re.search(r"[A-Z]", password):
        return "Password must contain at least one uppercase letter.", 400
    if not re.search(r"[0-9]", password):
        return "Password must contain at least one digit.", 400
    if not re.search(r"[!@#$%^&*(),.?\":{}|<>]", password):
        return "Password must contain at least one special character.", 400

    salt = os.urandom(16)
    hashed_password = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, 100000)

    db = MySQLdb.connect(
        host=os.getenv("DB_HOST", "localhost"),
        user=os.getenv("DB_USER"),
        passwd=os.getenv("DB_PASSWORD"),
        db=os.getenv("DB_NAME"),
        ssl_mode="VERIFY_IDENTITY",
        ssl={
            "ca": os.getenv("DB_SSL_CA", "/path/to/ca.pem")
        }
    )
    cursor = db.cursor()
    try:
        cursor.execute("SELECT * FROM users WHERE username = %s", (username,))
        if cursor.fetchone() is not None:
            return "Username already exists", 409

        cursor.execute("INSERT INTO users (username, password, salt) VALUES (%s, %s, %s)", (username, hashed_password, salt))
        db.commit()
        return redirect('/')
    except MySQLdb.Error as e:
        db.rollback()
        print(f"Database error: {e}")
        return "Registration failed due to a database error.", 500
    finally:
        cursor.close()
        db.close()
