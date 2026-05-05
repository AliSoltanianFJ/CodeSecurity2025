
from flask import Flask, request, redirect, session, abort
import os
import MySQLdb
from werkzeug.security import check_password_hash

app = Flask(__name__)
app.secret_key = os.environ['FLASK_SECRET_KEY']
app.config.update(
    SESSION_COOKIE_SECURE=True,
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SAMESITE='Lax'
)


@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        abort(400)  # Bad Request

    try:
        db = MySQLdb.connect(
            host=os.environ['DB_HOST'],
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASS'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4',
            cursorclass=MySQLdb.cursors.DictCursor
        )
        with db.cursor() as cursor:
            cursor.execute(
                "SELECT password_hash FROM users WHERE username = %s",
                (username,)
            )
            user = cursor.fetchone()
        db.close()

        if user and check_password_hash(user['password_hash'], password):
            session['username'] = username
            return redirect('/dashboard')
    except MySQLdb.Error:
        abort(500)  # Internal Server Error

    abort(403)  # Forbidden
