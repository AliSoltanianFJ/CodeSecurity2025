from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""

from flask import session, redirect, request
from werkzeug.security import check_password_hash

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        return redirect('/login?error=missing')

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        cursor.execute(
            "SELECT id, password_hash FROM users WHERE username = %s",
            (username,)
        )
        row = cursor.fetchone()
    except MySQLdb.MySQLError:
        return "Internal server error", 500
    finally:
        cursor.close()
        db.close()

    if row is None:
        return redirect('/login?error=invalid')

    user_id, stored_hash = row
    if not check_password_hash(stored_hash, password):
        return redirect('/login?error=invalid')

    session.clear()
    session['user_id'] = user_id
    return redirect('/dashboard')