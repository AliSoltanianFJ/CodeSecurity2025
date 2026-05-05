# ...existing code...
from flask import Flask, request, abort
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
        abort(400)

    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        abort(400)

    if len(password) < 12 or len(password) > 128 or re.search(r'\s', password):
        abort(400)

    password_hash = generate_password_hash(password)

    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')
    if not all([db_host, db_user, db_pass, db_name]):
        abort(500)

    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4', use_unicode=True)
    except Exception:
        abort(500)

    try:
        cur = conn.cursor()
        try:
            cur.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", (username, password_hash))
            conn.commit()
        except MySQLdb.IntegrityError:
            conn.rollback()
            abort(409)
        finally:
            cur.close()
    finally:
        conn.close()

    return '', 201
# ...existing code...
