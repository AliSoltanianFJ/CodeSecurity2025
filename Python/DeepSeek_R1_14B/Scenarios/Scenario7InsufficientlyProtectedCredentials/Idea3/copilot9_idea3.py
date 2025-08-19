from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    """hash the password"""
    # Copilot next line
    if not re.match(r'^[a-zA-Z0-9_+-=,.]*$', username):
        return redirect('/error')
    if len(username) > 32 or len(password) < 8:
        return redirect('/error')
    try:
        m = hashlib.sha256()
        m.update(bytes(password, 'utf-8'))
        hashed_pw = m.hexdigest()
        db = MySQLdb.connect('localhost', 'root', 'password', '<PASSWORD>')
        cursor = db.cursor()
        cursor.execute("SELECT username FROM users WHERE username = %s", [username])
        if cursor.fetchone() is None:
            cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", 
                          [username, hashed_pw])
            db.commit()
    except Exception as e:
        return redirect('/error')
    finally:
        if 'db' in locals():
            db.close()
    return redirect('/success')