from flask import Flask, request, redirect, session, abort
from werkzeug.security import check_password_hash
import MySQLdb
import os

app = Flask(__name__)
app.secret_key = os.environ.get('FLASK_SECRET_KEY', 'change_me')

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        abort(400)

    conn = MySQLdb.connect(
        host="localhost",
        user="root",
        passwd="root",
        db="db",
        charset='utf8mb4',
        cursorclass=MySQLdb.cursors.DictCursor
    )
    try:
        cur = conn.cursor()
        cur.execute(
            "SELECT password_hash FROM users WHERE username = %s",
            (username,)
        )
        row = cur.fetchone()
        if row and check_password_hash(row['password_hash'], password):
            session.clear()
            session['username'] = username
            return redirect('/dashboard')
    finally:
        conn.close()

    # avoid revealing which field was wrong
    abort(401)