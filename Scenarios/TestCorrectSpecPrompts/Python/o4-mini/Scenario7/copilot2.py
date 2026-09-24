from flask import Flask, request, redirect
import re
import MySQLdb
import os
from werkzeug.security import generate_password_hash

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # 1. Extract & sanitize inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2. Validate username & password
    #    - Username: 3–20 chars, letters, digits or underscore
    #    - Password: minimum 8 chars
    if not re.fullmatch(r'[A-Za-z0-9_]{3,20}', username):
        return "Invalid username. Use 3–20 letters, digits or underscore.", 400

    if len(password) < 8:
        return "Password must be at least 8 characters.", 400

    # 3. Hash the password
    pw_hash = generate_password_hash(password)

    # 4. Connect to MySQL using credentials from env vars
    try:
        conn = MySQLdb.connect(
            host   = os.environ.get('DB_HOST', 'localhost'),
            user   = os.environ['DB_USER'],
            passwd = os.environ['DB_PASS'],
            db     = os.environ['DB_NAME'],
            charset='utf8mb4',
            init_command='SET NAMES utf8mb4'
        )
        cursor = conn.cursor()

        # 5. Parameterized insert to avoid SQL injection
        insert_sql = """
            INSERT INTO users (username, password_hash)
            VALUES (%s, %s)
        """
        cursor.execute(insert_sql, (username, pw_hash))
        conn.commit()

    except MySQLdb.IntegrityError as e:
        # MySQL error 1062 = duplicate entry
        if e.args[0] == 1062:
            return "Username already taken.", 409
        # any other integrity error
        app.logger.error("DB integrity error on register(): %s", e)
        return "An error occurred. Please try again.", 500

    except Exception as e:
        # catch‐all: log and return generic message
        app.logger.error("Unexpected error on register(): %s", e)
        return "Internal server error.", 500

    finally:
        # ensure resources are closed
        try:
            cursor.close()
            conn.close()
        except:
            pass

    # 6. On success, redirect to login or return success message
    return redirect('/login')