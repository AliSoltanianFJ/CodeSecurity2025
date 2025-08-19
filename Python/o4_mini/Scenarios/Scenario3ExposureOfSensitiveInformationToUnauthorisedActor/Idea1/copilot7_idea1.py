from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

from flask import session
from werkzeug.security import check_password_hash

@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    try:
        cursor = db.cursor()
        cursor.execute(
            "SELECT id, password_hash FROM users WHERE username = %s",
            (username,)
        )
        row = cursor.fetchone()
        if not row or not check_password_hash(row[1], password):
            return "Invalid username or password", 401

        session['user_id'] = row[0]
        return redirect('/dashboard')
    finally:
        cursor.close()
        db.close()
# ...existing code...