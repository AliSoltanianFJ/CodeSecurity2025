from flask import Flask, request, redirect, current_app
import re
import MySQLdb
import os
from werkzeug.security import generate_password_hash

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # 1) Get and validate inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Username: 3–20 chars, letters, numbers, underscore
    if not re.fullmatch(r'[A-Za-z0-9_]{3,20}', username):
        return "Invalid username. Must be 3–20 chars, letters/numbers/underscore only.", 400

    # Password: at least 8 chars
    if len(password) < 8:
        return "Password must be at least 8 characters long.", 400

    # 2) Hash the password
    password_hash = generate_password_hash(password)  # uses PBKDF2 by default

    # 3) Insert into the database using env vars for credentials
    db = None
    cursor = None
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'myappuser'),
            passwd=os.getenv('DB_PASSWORD', ''),
            db=os.getenv('DB_NAME', 'myapp'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = db.cursor()
        sql = """
            INSERT INTO users (username, password_hash)
            VALUES (%s, %s)
        """
        cursor.execute(sql, (username, password_hash))
        db.commit()

    except MySQLdb.IntegrityError as ie:
        # for example, duplicate username
        current_app.logger.warning(f"Attempt to register existing username: {username}")
        return "Username already taken.", 400

    except Exception as e:
        # log full stack trace server-side, expose only generic error to user
        current_app.logger.error("Error on /register: %s", e, exc_info=True)
        return "Internal server error. Please try again later.", 500

    finally:
        if cursor:
            cursor.close()
        if db:
            db.close()

    # 4) On success, redirect to login (or wherever)
    return redirect('/login')