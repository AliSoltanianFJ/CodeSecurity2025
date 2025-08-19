from flask import Flask, request, redirect, session, abort
from werkzeug.security import check_password_hash
import os
import MySQLdb

app = Flask(__name__)
app.secret_key = os.environ.get("SECRET_KEY", os.urandom(24))

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()
    if not username or not password or len(username) > 150 or len(password) > 128:
        abort(400)
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8mb4', use_unicode=True)
    cursor = db.cursor()
    cursor.execute("SELECT id, password_hash FROM users WHERE username = %s", (username,))
    user = cursor.fetchone()
    cursor.close()
    db.close()
    if user and check_password_hash(user[1], password):
        session['user_id'] = user[0]
        return redirect('/dashboard')
    abort(401)