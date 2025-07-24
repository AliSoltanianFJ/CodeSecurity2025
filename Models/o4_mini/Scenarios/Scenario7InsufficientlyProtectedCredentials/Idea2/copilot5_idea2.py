from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
import bcrypt
from flask import abort
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Validate username: only letters, digits, underscores; 3–30 chars
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        abort(400)

    # Validate password length: 8–128 chars
    if len(password) < 8 or len(password) > 128:
        abort(400)

    # Securely hash password with bcrypt
    salt = bcrypt.gensalt()
    pwd_hash = bcrypt.hashpw(password.encode('utf-8'), salt)

    # Connect using env vars, use utf8mb4
    db = MySQLdb.connect(
        host=os.environ.get('DB_HOST', 'localhost'),
        user=os.environ.get('DB_USER'),
        passwd=os.environ.get('DB_PASS'),
        db=os.environ.get('DB_NAME'),
        charset='utf8mb4',
        use_unicode=True
    )
    cursor = db.cursor()
    try:
        cursor.execute(
            'INSERT INTO users (username, password) VALUES (%s, %s)',
            (username, pwd_hash.decode('utf-8'))
        )
        db.commit()
    except MySQLdb.IntegrityError:
        db.rollback()
        abort(409)
    except MySQLdb.Error:
        db.rollback()
        abort(500)
    finally:
        cursor.close()
        db.close()

    return redirect('/login')