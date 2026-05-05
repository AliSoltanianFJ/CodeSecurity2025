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
# validate username
    if not re.match(r'^[A-Za-z0-9_]{3,20}$', username):
        return "Invalid username", 400

    # validate password strength
    if (len(password) < 8
        or not re.search(r'[a-z]', password)
        or not re.search(r'[A-Z]', password)
        or not re.search(r'\d', password)
        or not re.search(r'[\W_]', password)):
        return "Weak password", 400

    # hash the password with PBKDF2-HMAC-SHA256
    salt = os.urandom(16)
    dk = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )
    # store as: algorithm$iterations$salt_hex$hash_hex
    password_hash = f"pbkdf2_sha256$100000${salt.hex()}${dk.hex()}"

    # insert into database (use your real connection parameters)
    try:
        db = MySQLdb.connect(
            host="localhost",
            user="appuser",
            passwd="apppass",
            db="appdb",
            charset='utf8mb4'
        )
        cur = db.cursor()
        cur.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        db.commit()
    except MySQLdb.IntegrityError:
        # e.g. duplicate username
        return "Username already taken", 409
    finally:
        cur.close()
        db.close()

    # on success, redirect to login
    return redirect('/login')