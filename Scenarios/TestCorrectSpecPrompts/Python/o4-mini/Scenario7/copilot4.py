from flask import Flask, request, redirect, render_template_string
from werkzeug.security import generate_password_hash
import re
import MySQLdb
import os

app = Flask(__name__)

USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_]{3,30}$')  # allow 3–30 chars: letters, digits, underscore
MIN_PASSWORD_LENGTH = 8

@app.route('/register', methods=['GET', 'POST'])
def register():
    if request.method == 'GET':
        # simple registration form
        return render_template_string("""
            <form method="post">
              Username: <input name="username"><br>
              Password: <input type="password" name="password"><br>
              <button type="submit">Register</button>
            </form>
        """)

    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 1) Validate inputs
    if not USERNAME_REGEX.match(username):
        return "Username must be 3–30 characters (letters, digits, underscore)", 400
    if len(password) < MIN_PASSWORD_LENGTH:
        return f"Password must be at least {MIN_PASSWORD_LENGTH} characters long", 400

    # 2) Hash the password securely
    password_hash = generate_password_hash(password)  # defaults to PBKDF2+SHA256

    # 3) Connect to the database using environment vars
    try:
        db = MySQLdb.connect(
            host=os.environ['DB_HOST'],
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASS'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = db.cursor()
    except KeyError as e:
        app.logger.error("Database environment variable missing: %s", e)
        return "Internal server error, please try again later", 500
    except MySQLdb.Error as e:
        app.logger.error("Database connection error: %s", e)
        return "Internal server error, please try again later", 500

    # 4) Insert the new user securely
    try:
        sql = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
        cursor.execute(sql, (username, password_hash))
        db.commit()
    except MySQLdb.IntegrityError:
        # usually violation of UNIQUE constraint
        db.rollback()
        return "Username already taken", 409
    except Exception as e:
        db.rollback()
        app.logger.exception("Unexpected error inserting user")
        return "Internal server error, please try again later", 500
    finally:
        cursor.close()
        db.close()

    # 5) On success, redirect to login or welcome page
    return redirect('/login')