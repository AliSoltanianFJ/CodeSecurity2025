from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
from werkzeug.security import generate_password_hash

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])

@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    # Validate username: only alphanumeric, between 3 and 20 characters
    if not re.match(r'^[A-Za-z0-9]{3,20}$', username):
        return "Invalid username", 400
    # Validate password complexity: min 8 chars, uppercase, lowercase, digit
    if (len(password) < 8 or
        not re.search(r'[A-Z]', password) or
        not re.search(r'[a-z]', password) or
        not re.search(r'\d', password)):
        return "Password must be at least 8 characters long and include uppercase, lowercase, and digit", 400
    # Hash password with PBKDF2 + salt
    hashed_password = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)
    # Connect to database
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'root'),
        passwd=os.getenv('DB_PASS', ''),
        db=os.getenv('DB_NAME', 'users_db'),
        charset='utf8mb4',
        use_unicode=True
    )
    cursor = db.cursor()
    try:
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()
    except MySQLdb.IntegrityError:
        db.rollback()
        return "Username already exists", 409
    except Exception as e:
        db.rollback()
        return f"Registration failed: {e}", 500
    finally:
        cursor.close()
        db.close()
    return redirect('/login')