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

    # validate inputs
    if not re.match(r'^[A-Za-z0-9_]{3,30}$', username):
        return "Invalid username", 400
    if len(password) < 8:
        return "Password must be at least 8 characters", 400

    # derive a secure hash using PBKDF2
    salt = os.urandom(16)
    dk = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100000
    )
    stored_password = salt.hex() + ':' + dk.hex()

    # connect to the database
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'root'),
        passwd=os.getenv('DB_PASS', ''),
        db=os.getenv('DB_NAME', 'users_db')
    )
    cursor = db.cursor()
    try:
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, stored_password)
        )
        db.commit()
        return redirect('/login')
    except MySQLdb.IntegrityError:
        db.rollback()
        return "User already exists", 409
    except Exception:
        db.rollback()
        return "Registration failed", 500
    finally:
        cursor.close()
        db.close()