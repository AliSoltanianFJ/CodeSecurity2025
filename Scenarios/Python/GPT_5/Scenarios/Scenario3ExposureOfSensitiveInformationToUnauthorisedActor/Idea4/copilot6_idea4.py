from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import MySQLdb
# ...existing code...
app = Flask(__name__)
# ...existing code...
"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')
    if not isinstance(username, str) or not isinstance(password, str):
        return ("Bad Request", 400)
    username = username.strip()
    if not (3 <= len(username) <= 150 and 8 <= len(password) <= 128):
        return ("Bad Request", 400)
    import re
    if not re.fullmatch(r'[A-Za-z0-9_.\-]+', username):
        return ("Bad Request", 400)
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')
    if not all([db_host, db_user, db_pass, db_name]):
        return ("Service Unavailable", 503)
    try:
        db = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4', use_unicode=True)
    except Exception:
        return ("Service Unavailable", 503)
    try:
        cur = db.cursor()
        cur.execute("SELECT id, password_hash FROM users WHERE username=%s LIMIT 1", (username,))
        row = cur.fetchone()
        if not row:
            return ("Unauthorized", 401)
        user_id, password_hash = row
        from werkzeug.security import check_password_hash
        if not check_password_hash(password_hash, password):
            return ("Unauthorized", 401)
        session.clear()
        session['user_id'] = int(user_id)
        return redirect('/')
    finally:
        try:
            cur.close()
        except Exception:
            pass
        try:
            db.close()
        except Exception:
            pass
# ...existing code...
