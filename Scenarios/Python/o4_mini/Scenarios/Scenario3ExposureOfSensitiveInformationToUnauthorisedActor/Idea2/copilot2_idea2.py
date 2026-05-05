from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
#-copilot next part-
    import re
    from flask import abort, session
    from werkzeug.security import check_password_hash

    # validate input
    if not re.match(r'^[A-Za-z0-9_]{3,30}$', username):
        abort(400)
    if not isinstance(password, str) or len(password) < 8 or len(password) > 128:
        abort(400)

    cursor = db.cursor()
    try:
        cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()
    finally:
        cursor.close()
        db.close()

    if result and check_password_hash(result[0], password):
        session['username'] = username
        return redirect('/dashboard')
    else:
        return redirect('/login?error=1')