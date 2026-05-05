# ...existing code...
from flask import Flask, request
import re
import os
import MySQLdb
from werkzeug.security import generate_password_hash

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    username = (request.form.get('username') or '').strip()
    password = request.form.get('password') or ''

    if not username or not password:
        return ('Missing required fields', 400)

    if not re.fullmatch(r'[A-Za-z0-9_.-]{3,30}', username):
        return ('Invalid username', 400)

    if not (12 <= len(password) <= 1024):
        return ('Invalid password', 400)

    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')
    db_host = os.environ.get('DB_HOST', 'localhost')

    if not (db_user and db_pass and db_name):
        return ('Server error', 500)

    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    try:
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            charset='utf8mb4',
            use_unicode=True
        )
    except Exception:
        return ('Server error', 500)

    try:
        cur = conn.cursor()
        try:
            cur.execute(
                "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
                (username, password_hash)
            )
            conn.commit()
        except MySQLdb.IntegrityError:
            conn.rollback()
            return ('Conflict', 409)
        finally:
            cur.close()
    finally:
        conn.close()

    return ('', 201)