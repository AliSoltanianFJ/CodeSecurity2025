from flask import Flask, request, redirect, session, abort, url_for, flash
from flask_bcrypt import Bcrypt
import MySQLdb
import os

app = Flask(__name__)
app.secret_key = os.urandom(32)      # keep this consistent in production
bcrypt = Bcrypt(app)

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        flash('Invalid credentials')
        return redirect(url_for('show_login'))

    # Establish DB connection
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    try:
        # Parameterized query prevents SQL injection
        cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()
        if row and bcrypt.check_password_hash(row[0], password):
            # Login succeeded
            session.clear()
            session['username'] = username
            # Optional: regenerate session ID to prevent fixation
            # flask.session.regenerate()  # if supported
            return redirect(url_for('dashboard'))
        else:
            flash('Invalid credentials')   # generic message
            return redirect(url_for('show_login'))
    finally:
        cursor.close()
        db.close()