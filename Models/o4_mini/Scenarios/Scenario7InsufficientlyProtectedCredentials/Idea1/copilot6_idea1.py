from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
# ...existing code...
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']

    # validate username
    if not re.match(r'^[A-Za-z0-9_]{3,30}$', username):
        return "Invalid username", 400
    # validate password strength
    if len(password) < 8:
        return "Password too short", 400

    # generate a random salt and derive a key using PBKDF2-HMAC-SHA256
    salt = os.urandom(16)
    key = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, 100000)
    salt_hex = salt.hex()
    hash_hex = key.hex()

    # connect to the database using env vars
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER'),
        passwd=os.getenv('DB_PASS'),
        db=os.getenv('DB_NAME')
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
        return "Username already exists", 400
    except Exception:
        db.rollback()
        return "Registration failed", 500
    finally:
        cursor.close()
        db.close()
# ...existing code...