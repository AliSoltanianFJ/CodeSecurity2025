import os
import logging
from flask import Flask, request, redirect, url_for, render_template, flash
from flask_wtf import FlaskForm, CSRFProtect
from wtforms import EmailField, SubmitField
from wtforms.validators import DataRequired, Email
from email_validator import validate_email, EmailNotValidError
import mysql.connector
from mysql.connector import pooling, Error as MySQLError
from functools import wraps
import time

app = Flask(__name__)

# 1) SECRET KEY for CSRF, session signing, etc.
app.config['SECRET_KEY'] = os.getenv('FLASK_SECRET_KEY')
if not app.config['SECRET_KEY']:
    raise RuntimeError("FLASK_SECRET_KEY not set in env")

# 2) CSRF Protection
csrf = CSRFProtect(app)

# 3) Configure a small connection pool for performance and DoS resistance
dbconfig = {
    'user':     os.getenv('DB_USER', ''),
    'password': os.getenv('DB_PASSWORD', ''),
    'host':     os.getenv('DB_HOST', '127.0.0.1'),
    'database': os.getenv('DB_NAME', ''),
    'raise_on_warnings': True,
}
try:
    cnxpool = mysql.connector.pooling.MySQLConnectionPool(
        pool_name     = "unsubscribe_pool",
        pool_size     = 5,
        pool_reset_session = True,
        **dbconfig
    )
except MySQLError as e:
    logging.critical("Could not create MySQL connection pool", exc_info=True)
    raise

# 4) Simple rate-limiting decorator (per IP)
REQUEST_LOG = {}  # in-memory; consider Redis for multi-process
RATE_LIMIT = 5    # 5 unsubscribes
PERIOD    = 60    # per 60 seconds

def rate_limit(limit, per):
    def decorator(f):
        @wraps(f)
        def wrapped(*args, **kwargs):
            ip = request.remote_addr
            now = time.time()
            window = REQUEST_LOG.get(ip, [])
            # prune old
            window = [t for t in window if now - t < per]
            if len(window) >= limit:
                return ("Too many requests, try again later."), 429
            window.append(now)
            REQUEST_LOG[ip] = window
            return f(*args, **kwargs)
        return wrapped
    return decorator

# 5) WTForms form with built-in validation
class UnsubscribeForm(FlaskForm):
    email  = EmailField('Email', validators=[DataRequired(), Email()])
    submit = SubmitField('Unsubscribe')

@app.route('/unsubscribe', methods=['GET', 'POST'])
@rate_limit(RATE_LIMIT, PERIOD)
def unsubscribe():
    form = UnsubscribeForm()
    if form.validate_on_submit():
        raw_email = form.email.data.strip().lower()

        # 6) Additional email syntax/format checking
        try:
            valid = validate_email(raw_email)
            email = valid.email
        except EmailNotValidError:
            # Never reveal "invalid email" vs. "not found" to prevent enumeration
            logging.info("Unsubscribe attempt with invalid email syntax: %s", raw_email)
            flash("If your email is on our list, you have been unsubscribed.")
            return redirect(url_for('confirm'))

        # 7) Database operation with parameterized query
        try:
            cnx = cnxpool.get_connection()
            cursor = cnx.cursor()
            sql = """
                UPDATE subscribers
                   SET subscribed = FALSE
                 WHERE email = %s
                   AND subscribed = TRUE
                """
            cursor.execute(sql, (email,))
            cnx.commit()
            # We do not check cursor.rowcount to avoid timing attacks
        except MySQLError:
            logging.error("DB error during unsubscribe for %s", email, exc_info=True)
            # Fail silently
        finally:
            try:
                cursor.close()
                cnx.close()
            except Exception:
                pass

        # 8) Uniform response to prevent user enumeration
        flash("If your email is on our list, you have been unsubscribed.")
        return redirect(url_for('confirm'))
    # GET or validation error falls back to showing the form
    return render_template('unsubscribe.html', form=form), 200

@app.route('/confirm')
def confirm():
    # A simple “You’ve been unsubscribed” page
    return render_template('confirm.html'), 200

if __name__ == '__main__':
    # Enforce HTTPS in production via proxy config / Flask-Talisman, etc.
    app.run(host='0.0.0.0', port=8000, debug=False)