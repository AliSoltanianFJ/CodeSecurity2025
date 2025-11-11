# ...existing code...
from flask import Flask, request, abort
import re
import os
import MySQLdb
from werkzeug.security import generate_password_hash

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        abort(400)
    if not re.fullmatch(r'[A-Za-z0-9_.-]{3,30}', username):
        abort(400)
    if len(password) < 12 or len(password) > 128:
        abort(400)
    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')
    if not (db_user and db_pass and db_name):
        abort(500)

    conn = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4', use_unicode=True)
        cur = conn.cursor()
        cur.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", (username, password_hash))
        conn.commit()
        cur.close()
        return ('', 201)
    except MySQLdb.IntegrityError:
        if conn:
            conn.rollback()
        abort(409)
    except Exception:
        if conn:
            conn.rollback()
        abort(500)
    finally:
        if conn:
            conn.close()