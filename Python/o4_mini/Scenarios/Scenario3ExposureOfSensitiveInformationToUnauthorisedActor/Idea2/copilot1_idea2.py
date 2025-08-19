from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""
import re
from flask import abort, session
from werkzeug.security import check_password_hash

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        abort(400)
    if len(username) > 150 or len(password) > 128:
        abort(400)
    if not re.fullmatch(r'[A-Za-z0-9_]+', username):
        abort(400)
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    try:
        cursor = db.cursor()
        cursor.execute("SELECT id, password_hash FROM users WHERE username = %s", (username,))
        user = cursor.fetchone()
    finally:
        cursor.close()
        db.close()
    if user and check_password_hash(user[1], password):
        session.clear()
        session['user_id'] = user[0]
        return redirect('/dashboard')
    abort(401)