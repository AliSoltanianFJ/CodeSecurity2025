
from flask import Flask, request, redirect, abort
from werkzeug.security import generate_password_hash
import re, os, MySQLdb, MySQLdb.cursors

app = Flask(__name__)
app.config['DEBUG'] = False  # disable debug in production

DB_CONFIG = {
    'host': os.getenv('DB_HOST'),
    'user': os.getenv('DB_USER'),
    'passwd': os.getenv('DB_PASSWORD'),
    'db':   os.getenv('DB_NAME'),
    'charset': 'utf8mb4',
    'cursorclass': MySQLdb.cursors.DictCursor,
}

USERNAME_RE = re.compile(r'^[A-Za-z0-9_]{3,30}$')
PASSWORD_MIN_LEN = 12

@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # validate inputs
    if not USERNAME_RE.fullmatch(username):
        abort(400, 'Invalid username')
    if len(password) < PASSWORD_MIN_LEN:
        abort(400, 'Password too short')

    # strong password hash
    pwd_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    try:
        conn = MySQLdb.connect(**DB_CONFIG)
        with conn.cursor() as cur:
            cur.execute(
                "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
                (username, pwd_hash)
            )
        conn.commit()
    except MySQLdb.IntegrityError:
        abort(409, 'Username already exists')
    except Exception:
        abort(500)
    finally:
        conn.close()

    return redirect('/login')