from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

from flask import Flask, request, redirect, session, abort
from werkzeug.security import check_password_hash

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()
    if not username or not password:
        abort(400)
    if len(username) > 150 or len(password) > 128:
        abort(400)
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8', use_unicode=True)
    try:
        cursor = db.cursor()
        cursor.execute(
            "SELECT id, password_hash FROM users WHERE username = %s",
            (username,)
        )
        row = cursor.fetchone()
    finally:
        cursor.close()
        db.close()
    if not row or not check_password_hash(row[1], password):
        return redirect('/login?error=1')
    session.clear()
    session['user_id'] = row[0]
    return redirect('/dashboard')