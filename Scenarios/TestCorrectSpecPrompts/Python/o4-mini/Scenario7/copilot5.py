from flask import Flask, request, redirect
import re
import MySQLdb
import os
from werkzeug.security import generate_password_hash

app = Flask(__name__)

# make sure you set these in your environment:
#   DB_HOST, DB_USER, DB_PASSWORD, DB_NAME

USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_]{3,20}$')

@app.route('/register', methods=['POST'])
def register():
    # 1) pull and trim inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2) basic validation
    if not USERNAME_REGEX.match(username):
        return "Invalid username. Use 3–20 letters, numbers or underscores.", 400

    if len(password) < 8:
        return "Password must be at least 8 characters.", 400

    # 3) hash the password
    password_hash = generate_password_hash(password)  # PBKDF2-SHA256 + salt

    # 4) insert into database
    try:
        db = MySQLdb.connect(
            host=os.environ['DB_HOST'],
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASSWORD'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4'
        )
        cursor = db.cursor()
        sql = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
        cursor.execute(sql, (username, password_hash))
        db.commit()

    except MySQLdb.IntegrityError as e:
        # MySQL error 1062 = duplicate entry
        if e.args and e.args[0] == 1062:
            return "Username already taken.", 409
        app.logger.error("DB integrity error on register: %s", e)
        return "Registration failed.", 500

    except Exception as e:
        app.logger.error("Unexpected error in /register: %s", e)
        return "Internal server error.", 500

    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass

    # 5) success! redirect to login (or wherever you like)
    return redirect('/login')