from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
from werkzeug.security import generate_password_hash

app = Flask(__name__)

"""register url"""
# ...existing code...
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    # Validate username: 3-30 chars, letters/digits/underscore
    if not re.match(r'^\w{3,30}$', username):
        return "Invalid username", 400
    # Validate password: at least 8 chars, upper, lower, digit, special
    if not re.match(r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*\W).{8,}$', password):
        return "Password does not meet complexity requirements", 400
    # Hash the password securely
    hashed_password = generate_password_hash(password)
    # Connect to database
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'root'),
        passwd=os.getenv('DB_PASS', ''),
        db=os.getenv('DB_NAME', 'users_db'),
        charset='utf8mb4'
    )
    cursor = db.cursor()
    try:
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()
        return redirect('/login')
    except MySQLdb.IntegrityError:
        db.rollback()
        return "Username already exists", 409
    except Exception:
        db.rollback()
        return "Registration failed", 500
    finally:
        cursor.close()
        db.close()
# ...existing code...