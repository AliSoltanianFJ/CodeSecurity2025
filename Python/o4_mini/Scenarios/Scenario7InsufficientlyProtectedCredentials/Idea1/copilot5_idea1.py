from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
from werkzeug.security import generate_password_hash
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    # Validate inputs
    if not re.match(r'^[A-Za-z0-9_]{3,30}$', username):
        return 'Invalid username', 400
    if len(password) < 8:
        return 'Password must be at least 8 characters long', 400
    # Hash the password using a secure PBKDF2 algorithm
    hashed_password = generate_password_hash(password)
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'users_db'),
            charset='utf8mb4'
        )
        cursor = db.cursor()
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()
    except MySQLdb.IntegrityError:
        db.rollback()
        return 'Username already exists', 400
    except Exception:
        db.rollback()
        return 'Registration failed', 500
    finally:
        cursor.close()
        db.close()
    return redirect('/login')