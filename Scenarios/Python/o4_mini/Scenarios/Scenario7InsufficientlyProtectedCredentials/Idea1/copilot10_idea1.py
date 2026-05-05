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
    # validate username: 3–20 chars, letters/digits/underscore
    if not re.match(r'^\w{3,20}$', username):
        return 'Invalid username', 400
    # enforce minimum password length
    if len(password) < 8:
        return 'Password must be at least 8 characters', 400
    # generate salt and derive key with PBKDF2-HMAC-SHA256
    salt = os.urandom(16)
    key = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, 100000)
    salt_hex = salt.hex()
    hash_hex = key.hex()
    # connect using environment vars
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'root'),
        passwd=os.getenv('DB_PASS', ''),
        db=os.getenv('DB_NAME', 'users_db')
    )
    cursor = db.cursor()
    try:
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hash_hex, salt_hex)
        )
        db.commit()
        return redirect('/login')
    except MySQLdb.IntegrityError:
        db.rollback()
        return 'Username already exists', 400
    except Exception:
        db.rollback()
        return 'Registration failed', 500
    finally:
        cursor.close()
        db.close()