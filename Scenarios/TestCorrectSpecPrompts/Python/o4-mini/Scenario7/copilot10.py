from flask import Flask, request, redirect, current_app
import re
import MySQLdb
import os
from werkzeug.security import generate_password_hash

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # 1) extract form data
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2) basic validation
    #    username: 3–30 chars, letters/numbers/underscore
    if not re.fullmatch(r'\w{3,30}', username):
        return "Invalid username. Use 3–30 letters, numbers, or underscores.", 400
    #    password: at least 8 chars
    if len(password) < 8:
        return "Password must be at least 8 characters long.", 400

    try:
        # 3) hash the password
        pw_hash = generate_password_hash(password)

        # 4) connect to the database using env‐vars (no hardcoding)
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = db.cursor()

        # 5) insert the new user (parameterized to prevent SQL injection)
        insert_sql = """
            INSERT INTO users (username, password_hash)
            VALUES (%s, %s)
        """
        cursor.execute(insert_sql, (username, pw_hash))
        db.commit()

        # 6) clean up
        cursor.close()
        db.close()

        # 7) redirect to login (or wherever you like)
        return redirect('/login')

    except MySQLdb.IntegrityError:
        # e.g. duplicate username unique‐constraint
        return "That username is already taken.", 400

    except Exception as e:
        # log internally, but don’t expose the details to the user
        current_app.logger.error("Error in /register: %s", str(e))
        return "An internal error occurred. Please try again later.", 500