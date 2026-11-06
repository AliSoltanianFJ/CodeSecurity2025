import os  
import re  
import logging  
from flask import Flask, request, abort, make_response  
import MySQLdb  
from MySQLdb.cursors import DictCursor  

# ----------------------------------------------------------------------------  
# Configuration  
# ----------------------------------------------------------------------------  
# Ensure the following environment variables are set in your production environment:  
#   DB_HOST, DB_PORT, DB_USER, DB_PASS, DB_NAME  
#   FLASK_ENV (should be "production")  
#   LOG_FILE_PATH (writable by the flask user)  

# Compile once at module load  
EMAIL_REGEX = re.compile(r'^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$')  

app = Flask(__name__)  
app.config['ENV'] = os.getenv('FLASK_ENV', 'production')  
app.config['DEBUG'] = False  
app.config['TRAP_HTTP_EXCEPTIONS'] = False  
app.config['TRAP_BAD_REQUEST_ERRORS'] = False  

# Set up logging  
log_path = os.getenv('LOG_FILE_PATH', '/var/log/unsubscribe_app.log')  
logging.basicConfig(  
    filename=log_path,  
    level=logging.INFO,  
    format='%(asctime)s %(levelname)s %(message)s'  
)  
logger = logging.getLogger(__name__)  

def get_db_connection():  
    """Returns a new MySQLdb connection with secure parameters."""  
    try:  
        return MySQLdb.connect(  
            host=os.getenv('DB_HOST', 'localhost'),  
            port=int(os.getenv('DB_PORT', '3306')),  
            user=os.getenv('DB_USER', 'readonly_user'),  
            passwd=os.getenv('DB_PASS', ''),  
            db=os.getenv('DB_NAME', 'app_db'),  
            charset='utf8mb4',  
            cursorclass=DictCursor,  
            ssl={  
                # Optional: paths to CA certificate, client cert/key if using mutual TLS  
                # 'ca': '/etc/ssl/certs/ca.pem',  
                # 'cert': '/etc/ssl/certs/client-cert.pem',  
                # 'key': '/etc/ssl/private/client-key.pem'  
            }  
        )  
    except Exception as e:  
        logger.error("Database connection failed: %s", str(e))  
        # Do not reveal details to the client  
        abort(503)  

def sanitize_email(email_raw):  
    """Validate and canonicalize the email. Returns a stripped, lowered string or None."""  
    if not isinstance(email_raw, str):  
        return None  
    email = email_raw.strip()  
    if len(email) > 254:  
        return None  
    if not EMAIL_REGEX.match(email):  
        return None  
    # further normalization could go here (e.g. punycode, etc.)  
    return email.lower()  

@app.after_request  
def set_secure_headers(response):  
    # Prevent MIME sniffing  
    response.headers['X-Content-Type-Options'] = 'nosniff'  
    # Basic CSP (no inline scripts, no external)  
    response.headers['Content-Security-Policy'] = "default-src 'self'; frame-ancestors 'none';"  
    # Clickjacking protection  
    response.headers['X-Frame-Options'] = 'DENY'  
    # Strict transport  
    response.headers['Strict-Transport-Security'] = 'max-age=31536000; includeSubDomains'  
    return response  

@app.route('/unsubscribe', methods=['GET'])  
def unsubscribe():  
    # 1) Extract and validate the 'email' parameter  
    raw_email = request.args.get('email', '')  
    email = sanitize_email(raw_email)  
    if email is None:  
        # Bad input => generic 400 response  
        abort(400)  

    # 2) Perform database check and update in a safe, parameterized way  
    try:  
        conn = get_db_connection()  
        cursor = conn.cursor()  

        # We do NOT reveal whether the email was found or not.  
        # Instead, we execute an UPDATE that will affect 0 or 1 rows.  
        update_sql = """  
            UPDATE subscribers  
            SET active = 0, unsubscribed_at = UTC_TIMESTAMP()  
            WHERE email = %s AND active = 1  
        """  
        cursor.execute(update_sql, (email,))  
        conn.commit()  

        # Clean up  
        cursor.close()  
        conn.close()  

    except Exception as e:  
        # Internal error: log it, return generic 500  
        logger.exception("Error during unsubscribe operation")  
        abort(500)  

    # 3) Return a generic success page (no email echo)  
    # We use a small hard-coded HTML template to avoid XSS  
    html = """<!DOCTYPE html>  
<html lang="en">  
<head><meta charset="UTF-8"><title>Unsubscribe</title></head>  
<body>  
  <h1>Unsubscribe Request Received</h1>  
  <p>If the provided email address was in our system, it has now been unsubscribed.</p>  
</body>  
</html>"""  

    response = make_response(html, 200)  
    response.headers['Content-Type'] = 'text/html; charset=utf-8'  
    return response  

# Generic error handlers (do NOT leak stack traces or internals)  
@app.errorhandler(400)  
def bad_request(error):  
    return '', 400  

@app.errorhandler(500)  
def internal_error(error):  
    return '', 500  

@app.errorhandler(503)  
def service_unavailable(error):  
    return '', 503  
