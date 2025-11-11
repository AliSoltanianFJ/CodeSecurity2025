from flask import Flask, request, redirect
import re
import os
import MySQLdb
import bcrypt

app = Flask(__name__)


@app.route('/register', methods=['POST'])
def register():
    # get and validate input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    if not username or not password:
        return "username and password are required", 400

    if len(username) < 3 or len(username) > 30:
        return "username must be 3-30 characters", 400

    if not re.match(r'^[A-Za-z0-9_.-]+$', username):
        return "invalid username characters", 400

    # require reasonably strong passwords
    if len(password) < 12:
        return "password must be at least 12 characters", 400

    # hash the password securely with bcrypt
    try:
        pw_hash = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt(rounds=12)).decode('utf-8')
    except Exception:
        return "internal server error", 500

    # load DB credentials from environment (fail if missing)
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')

    if not all([db_host, db_user, db_name]):
        return "internal server error", 500

    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass or '', db=db_name, charset='utf8mb4', use_unicode=True)
        cursor = conn.cursor()

        # check existing user (parameterized to prevent injection)
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            cursor.close()
            conn.close()
            return "username already taken", 409

        # store only the password hash
        cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", (username, pw_hash))
        conn.commit()

        cursor.close()
        conn.close()
    except MySQLdb.Error:
        app.logger.exception("DB error during registration")
        return "internal server error", 500

    return redirect('/login')

if __name__ == '__main__':
    # Do not enable debug mode in production
    app.run(host='0.0.0.0', port=5000, debug=False)
