from flask import Flask, request, redirect, render_template_string
from werkzeug.security import generate_password_hash
import MySQLdb
import os

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username')
    password = request.form.get('password')

    import re

    # Basic validation
    if not username or not password or not re.match(r'^[\w-]{3,50}$', username):
        return "Invalid username or password.", 400

    # Hash the password
    hashed_password = generate_password_hash(password)

    # Database credentials from environment
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER')
    db_passwd = os.environ.get('DB_PASSWORD')
    db_name = os.environ.get('DB_NAME')

    try:
        db = MySQLdb.connect(db_host, db_user, db_passwd, db_name)
        cursor = db.cursor()
        cursor.execute(
            "INSERT INTO users (username, password) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()
        cursor.close()
        db.close()
        return redirect('/login')
    except Exception as e:
        # Log exception internally (not sent to client)
        app.logger.error('Registration error: %s', e)
        return "An error occurred. Please try again later.", 500