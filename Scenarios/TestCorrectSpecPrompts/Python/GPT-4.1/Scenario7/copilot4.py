from flask import Flask, request, redirect, render_template, flash
import re
import MySQLdb
import os
from werkzeug.security import generate_password_hash

app = Flask(__name__)
app.secret_key = os.environ.get('SECRET_KEY', 'default_secret_key')

# Database connection (never hardcode credentials; example env use)
def get_db():
    return MySQLdb.connect(
        host=os.environ.get('DB_HOST', 'localhost'),
        user=os.environ.get('DB_USER', 'root'),
        passwd=os.environ.get('DB_PASSWORD', ''),
        db=os.environ.get('DB_NAME', 'test')
    )

@app.route('/register', methods=['POST'])
def register():
    try:
        username = request.form['username']
        password = request.form['password']

        # Basic input validation
        if not username or not password:
            flash('Username and password are required.')
            return redirect('/register')
        if not re.match(r'^\w+$', username):
            flash('Username must contain only letters, numbers, and underscores.')
            return redirect('/register')

        # Hash the password securely
        password_hash = generate_password_hash(password)

        # Store in DB securely (use parameterized query)
        db = get_db()
        cursor = db.cursor()

        # Check if username already exists
        cursor.execute("SELECT id FROM users WHERE username=%s", (username,))
        if cursor.fetchone():
            flash('Username already exists.')
            return redirect('/register')

        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        db.commit()

        flash('Registration successful.')
        return redirect('/login')
    except Exception:
        # Log the stack trace internally if needed, but do not expose to the user
        flash('An error occurred. Please try again later.')
        return redirect('/register')