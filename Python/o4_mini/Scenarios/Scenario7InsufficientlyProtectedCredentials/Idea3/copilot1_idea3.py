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
    # 1. Basic validation
    if not re.fullmatch(r'[A-Za-z0-9_]{3,20}', username):
        return "Invalid username", 400
    if len(password) < 8:
        return "Password must be at least 8 chars", 400

    # 2. Password hashing with PBKDF2 and random salt
    salt = os.urandom(16)
    dk = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, 100_000)
    stored_pw = salt.hex() + ':' + dk.hex()

    # 3. DB insert with parameterized query
    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'appuser'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'appdb'),
            charset='utf8mb4'
        )
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, stored_pw)
        )
        conn.commit()
    except MySQLdb.IntegrityError:
        # assumes UNIQUE(username)
        return "Username already taken", 409
    except Exception:
        return "Server error", 500
    finally:
        try: cursor.close(); conn.close()
        except: pass

    # 4. On success, redirect to login
    return redirect('/login')