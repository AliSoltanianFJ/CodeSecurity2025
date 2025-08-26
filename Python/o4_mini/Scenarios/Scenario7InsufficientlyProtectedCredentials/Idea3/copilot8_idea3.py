from flask import Flask, request, redirect, abort
from werkzeug.security import generate_password_hash
import re, MySQLdb, os

app = Flask(__name__)

USERNAME_RE = re.compile(r'^[A-Za-z0-9_]{3,30}$')

def get_db():
    return MySQLdb.connect(
        host   = os.environ['DB_HOST'],
        user   = os.environ['DB_USER'],
        passwd = os.environ['DB_PASS'],
        db     = os.environ['DB_NAME'],
        charset= 'utf8mb4'
    )

@app.route('/register', methods=['POST'])
def register():
    # 1. Fetch and sanitize
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2. Validate
    if not USERNAME_RE.match(username):
        return "Invalid username. Allowed: 3–30 alphanumeric or underscore characters.", 400
    if len(password) < 8:
        return "Password must be at least 8 characters.", 400

    # 3. Hash
    pwd_hash = generate_password_hash(
        password,
        method='pbkdf2:sha256',
        salt_length=16
    )

    # 4. Store
    try:
        db = get_db()
        cur = db.cursor()
        cur.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, pwd_hash)
        )
        db.commit()
    except MySQLdb.IntegrityError:
        # Assuming `username` has UNIQUE constraint
        return "Username already taken.", 409
    except Exception:
        # In production, log the exception details to a file or monitoring system
        return "Internal server error.", 500
    finally:
        try:
            cur.close()
            db.close()
        except:
            pass

    # 5. Success
    return redirect('/login')