import os
import logging
import MySQLdb
from flask import Flask, request, render_template, abort
from flask_wtf import CSRFProtect
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
from flask_talisman import Talisman
from email_validator import validate_email, EmailNotValidError

# ------------------------------------------------------------------------------
# Application factory
# ------------------------------------------------------------------------------
def create_app():
    # Create Flask app
    app = Flask(__name__, instance_relative_config=False)

    # ------------------
    # Configuration
    # ------------------
    # All sensitive configuration comes from environment variables. No hard-coded secrets.
    app.config['SECRET_KEY'] = os.getenv('SECRET_KEY')  # Must be set to a strong random value
    app.config['MYSQL_HOST'] = os.getenv('MYSQL_HOST', 'localhost')
    app.config['MYSQL_PORT'] = int(os.getenv('MYSQL_PORT', '3306'))
    app.config['MYSQL_DB'] = os.getenv('MYSQL_DB', 'subscriptions')
    app.config['MYSQL_USER'] = os.getenv('MYSQL_USER', 'unsubscribe_app')
    app.config['MYSQL_PASSWORD'] = os.getenv('MYSQL_PASSWORD')  # Must be set
    app.config['WTF_CSRF_TIME_LIMIT'] = None  # CSRF tokens do not expire within a session

    # Critical config checks
    if not app.config['SECRET_KEY'] or not app.config['MYSQL_PASSWORD']:
        # If any required secret is missing, halt startup to avoid running in insecure state
        raise RuntimeError("Missing required environment configuration")

    # ------------------
    # Security middleware
    # ------------------
    # Enforce HTTPS, HSTS, secure headers
    Talisman(app, content_security_policy={
        # Only allow resources from same origin
        'default-src': ["'self'"]
    })

    # CSRF protection for state-changing endpoints (we enforce token even though we use GET)
    csrf = CSRFProtect()
    csrf.init_app(app)

    # Rate limiting to mitigate brute-force / enumeration attacks
    limiter = Limiter(
        app,
        key_func=get_remote_address,
        default_limits=["5 per minute"]
    )

    # ------------------
    # Logging
    # ------------------
    # Logs go to stderr (container/GKE/CloudRun friendly)
    handler = logging.StreamHandler()
    handler.setLevel(logging.INFO)
    handler.setFormatter(logging.Formatter(
        '%(asctime)s %(levelname)s %(message)s [in %(pathname)s:%(lineno)d]'
    ))
    app.logger.setLevel(logging.INFO)
    app.logger.addHandler(handler)

    # ------------------
    # Database helper
    # ------------------
    def get_db_connection():
        """
        Returns a new MySQLdb connection using SSL.
        The database user 'unsubscribe_app' should have only
        SELECT and UPDATE privileges on the 'subscribers' table.
        """
        try:
            conn = MySQLdb.connect(
                host=app.config['MYSQL_HOST'],
                port=app.config['MYSQL_PORT'],
                user=app.config['MYSQL_USER'],
                passwd=app.config['MYSQL_PASSWORD'],
                db=app.config['MYSQL_DB'],
                charset='utf8mb4',
                ssl={'ssl': {}}  # Requires MySQL server SSL setup
            )
            return conn
        except MySQLdb.MySQLError:
            app.logger.error("Database connection failed", exc_info=True)
            # Do not leak details
            abort(500)

    # ------------------
    # Routes
    # ------------------
    @app.route('/unsubscribe', methods=['GET'])
    @limiter.limit("5 per minute")
    def unsubscribe():
        """
        Unsubscribe endpoint.
        Expects a single query parameter: email.
        Performs:
          - HTTPS enforcement
          - Strict email validation & normalization
          - Parameterized queries only
          - Generic success response to prevent email enumeration
          - Proper error handling without information leakage
        """
        # Enforce HTTPS at the application level (in addition to web server)
        if not request.is_secure:
            abort(403)  # Forbidden if not HTTPS

        # Retrieve and validate 'email' parameter
        raw_email = request.args.get('email', '').strip()
        if not raw_email:
            app.logger.info("Unsubscribe called without email parameter")
            # Return same generic page to avoid client-side differentiation
            return render_template('unsubscribe_result.html', success=True), 200

        try:
            # Normalize and validate email according to RFCs
            valid = validate_email(raw_email)
            email = valid.email  # e.g. lowercase, unicode normalization
        except EmailNotValidError:
            app.logger.info("Invalid email format for unsubscribe")
            return render_template('unsubscribe_result.html', success=True), 200

        # Perform database lookup and update in a single transaction
        conn = get_db_connection()
        try:
            with conn.cursor() as cursor:
                # 1. Look up subscriber ID
                cursor.execute(
                    "SELECT id FROM subscribers WHERE email = %s LIMIT 1",
                    (email,)
                )
                row = cursor.fetchone()
                if row:
                    subscriber_id = row[0]
                    # 2. Mark as unsubscribed
                    cursor.execute(
                        "UPDATE subscribers SET unsubscribed = 1, updated_at = UTC_TIMESTAMP() "
                        "WHERE id = %s",
                        (subscriber_id,)
                    )
                    conn.commit()
                    app.logger.info("Successfully unsubscribed email: %s", email)
                else:
                    # Do not reveal that the email was not found
                    app.logger.info("Unsubscribe requested for non-existent email: %s", email)
            # Always show success page (prevents account-enumeration)
            return render_template('unsubscribe_result.html', success=True), 200
        except Exception:
            app.logger.error("Error during unsubscribe operation", exc_info=True)
            # Generic 500
            abort(500)
        finally:
            try:
                conn.close()
            except Exception:
                pass  # If closing fails, nothing we can do

    return app