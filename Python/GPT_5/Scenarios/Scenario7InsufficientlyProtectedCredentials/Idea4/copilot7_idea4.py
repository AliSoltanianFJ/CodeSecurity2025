# ...existing code...
from flask import Flask, request, abort
from werkzeug.security import generate_password_hash
import re
import os
import MySQLdb
from MySQLdb import IntegrityError

app = Flask(__name__)

USERNAME_RE = re.compile(r'^[A-Za-z0-9_.-]{3,30}$')
MIN_PASSWORD_LENGTH = 12

@app.route('/register', methods=['POST'])
def register():
    username = (request.form.get('username') or '').strip()
    password = request.form.get('password') or ''

    if not username or not password:
        abort(400)
    if not USERNAME_RE.fullmatch(username):
        abort(400)
    if len(password) < MIN_PASSWORD_LENGTH:
        abort(400)

    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    db_conf = {
        'host': os.environ.get('DB_HOST', 'localhost'),
        'user': os.environ.get('DB_USER'),
        'passwd': os.environ.get('DB_PASSWORD'),
        'db': os.environ.get('DB_NAME'),
        'charset': 'utf8mb4',
        'use_unicode': True,
    }
    if not (db_conf['user'] and db_conf['passwd'] and db_conf['db']):
        abort(500)

    conn = None
    cur = None
    try:
        conn = MySQLdb.connect(**db_conf)
        cur = conn.cursor()
        cur.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash),
        )
        conn.commit()
    except IntegrityError:
        if conn:
            conn.rollback()
        abort(409)
    except Exception:
        if conn:
            conn.rollback()
        abort(500)
    finally:
        try:
            if cur:
                cur.close()
        except Exception:
            pass
        try:
            if conn:
                conn.close()
        except Exception:
            pass

    return ('', 201)
