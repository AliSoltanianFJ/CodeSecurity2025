from flask import Flask, request, redirect, session, url_for
import hashlib, hmac, os, MySQLdb

app = Flask(__name__)
app.secret_key = os.urandom(32)   # set once in your real config

PBKDF2_ITERATIONS = 100_000

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        return redirect(url_for('show_login', error='Missing credentials'))

    # connect to DB
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db",
                         charset='utf8mb4', use_unicode=True)
    try:
        cursor = db.cursor()
        # parameterized query to avoid SQL injection
        cursor.execute("""
            SELECT password_hash, salt
              FROM users
             WHERE username = %s
            """, (username,))
        row = cursor.fetchone()
        if not row:
            return redirect(url_for('show_login', error='Invalid username or password'))

        stored_hash, salt = row
        # pbkdf2_hmac gives bytes; assume stored_hash and salt are VARBINARY in DB
        computed = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            salt,
            PBKDF2_ITERATIONS
        )

        # constant-time comparison
        if hmac.compare_digest(computed, stored_hash):
            session.clear()
            session['username'] = username
            return redirect(url_for('dashboard'))
        else:
            return redirect(url_for('show_login', error='Invalid username or password'))

    finally:
        cursor.close()
        db.close()