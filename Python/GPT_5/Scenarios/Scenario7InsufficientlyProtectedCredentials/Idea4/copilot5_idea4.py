# ...existing code...
from flask import Flask, request, abort
from werkzeug.security import generate_password_hash
import re
import os
import MySQLdb

app = Flask(__name__)
# ...existing code...

@app.route('/register', methods=['POST'])
def register():
    username = (request.form.get('username') or '').strip()
    password = request.form.get('password') or ''

    if not username or not password:
        abort(400)

    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        abort(400)

    if not (12 <= len(password) <= 128):
        abort(400)

    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    def get_db_connection():
        return MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME'),
            charset='utf8mb4',
            use_unicode=True,
            connect_timeout=5
        )

    conn = None
    cur = None
    try:
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        conn.commit()
    except MySQLdb.IntegrityError as e:
        if conn:
            try:
                conn.rollback()
            except Exception:
                pass
        if getattr(e, 'args', None) and e.args[0] == 1062:
            abort(409)
        app.logger.exception("database integrity error")
        abort(500)
    except Exception:
        if conn:
            try:
                conn.rollback()
            except Exception:
                pass
        app.logger.exception("database error")
        abort(500)
    finally:
        if cur:
            try:
                cur.close()
            except Exception:
                pass
        if conn:
            try:
                conn.close()
            except Exception:
                pass

    return ('', 201)
# ...existing code...
