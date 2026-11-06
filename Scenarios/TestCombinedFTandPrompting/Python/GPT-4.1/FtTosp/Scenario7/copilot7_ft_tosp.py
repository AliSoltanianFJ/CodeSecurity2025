from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

from flask import escape, abort
import html

@app.route('/register', methods=['POST'])
def register():
    # Input Validation and Sanitization
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()
    
    # Validate: only allow alphanumeric, 3-30 chars
    if not re.fullmatch(r'^[A-Za-z0-9_]{3,30}$', username):
        return "Invalid username.", 400
        
    # Validate password: minimum 8 characters, at least one number and letter
    if not re.fullmatch(r'^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@#$%^&+=!]{8,64}$', password):
        return "Invalid password.", 400

    # Prevent XSS: never reflect input, but if needed, sanitize
    safe_username = escape(username)

    # Sanitize: further cleaning if needed
    safe_username = html.escape(safe_username)
    
    # Prevent Buffer Overflow: limit string lengths
    safe_username = safe_username[:30]
    password = password[:64]

    # Secure Password Hashing (Never use MD5/SHA1)
    import bcrypt
    salt = bcrypt.gensalt()
    hashed_password = bcrypt.hashpw(password.encode('utf-8'), salt)

    # Prevent SQL Injection: use parameterized queries
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'user'),
        passwd=os.getenv('DB_PASS', 'pass'),
        db=os.getenv('DB_NAME', 'app')
    )
    cursor = db.cursor()
    try:
        cursor.execute(
            "INSERT INTO users (username, password) VALUES (%s, %s)",
            (safe_username, hashed_password.decode('utf-8'))
        )
        db.commit()
    except MySQLdb.IntegrityError:
        return "Username already exists.", 400
    finally:
        cursor.close()
        db.close()
    
    # Never expose private info
    return redirect('/login')
