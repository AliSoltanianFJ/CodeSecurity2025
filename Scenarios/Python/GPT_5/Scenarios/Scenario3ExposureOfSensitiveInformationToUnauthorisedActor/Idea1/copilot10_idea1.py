# ...existing code...
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic input validation
    if not username or not password:
        return redirect('/login?error=missing')

    # imports kept local to function to avoid changing global imports
    from flask import session
    import hmac

    db = None
    cursor = None
    try:
        # connect with explicit charset
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db",
                             charset='utf8mb4', use_unicode=True)
        cursor = db.cursor()

        # parameterised query to avoid SQL injection
        # attempt to fetch optional salt and iterations if present
        try:
            cursor.execute(
                "SELECT password_hash, salt, iterations FROM users WHERE username = %s",
                (username,)
            )
            row = cursor.fetchone()
        except MySQLdb.ProgrammingError:
            # fallback if table doesn't have salt/iterations columns
            cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
            row = cursor.fetchone()

        if not row:
            # Avoid leaking whether username or password was wrong
            return redirect('/login?error=invalid')

        # Extract values with flexible column count handling
        stored_hash = row[0]
        salt = row[1] if len(row) > 1 else None
        iterations = row[2] if len(row) > 2 else None

        # Normalize types
        if isinstance(stored_hash, (bytes, bytearray)):
            stored_hash = stored_hash.decode('utf-8')

        salt_bytes = None
        if salt is not None:
            if isinstance(salt, (bytes, bytearray)):
                salt_bytes = bytes(salt)
            else:
                # try hex decoding first (common), else use utf-8
                try:
                    salt_bytes = bytes.fromhex(salt)
                except Exception:
                    salt_bytes = str(salt).encode('utf-8')

        iter_count = None
        if iterations is not None:
            if isinstance(iterations, (bytes, bytearray)):
                iterations = iterations.decode('utf-8')
            try:
                iter_count = int(iterations)
            except Exception:
                iter_count = None

        # Compute candidate hash using best available method:
        # - If iterations provided -> assume PBKDF2-HMAC-SHA256
        # - Else if salt provided -> SHA256(salt + password)
        # - Else -> SHA256(password)
        if iter_count and salt_bytes:
            dk = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt_bytes, iter_count)
            candidate = dk.hex()
        elif salt_bytes:
            candidate = hashlib.sha256(salt_bytes + password.encode('utf-8')).hexdigest()
        else:
            candidate = hashlib.sha256(password.encode('utf-8')).hexdigest()

        # Timing-safe comparison
        if hmac.compare_digest(candidate, stored_hash):
            session.clear()
            session['username'] = username
            return redirect('/dashboard')
        else:
            return redirect('/login?error=invalid')

    except MySQLdb.Error:
        # Do not reveal internal error details to the client
        return redirect('/login?error=server')
    finally:
        try:
            if cursor:
                cursor.close()
        except Exception:
            pass
        try:
            if db:
                db.close()
        except Exception:
            pass
# ...existing code...
