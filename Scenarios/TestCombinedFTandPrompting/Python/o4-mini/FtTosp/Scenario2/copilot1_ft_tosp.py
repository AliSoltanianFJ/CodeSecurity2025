from flask import Flask, request, redirect, render_template_string, abort  
from markupsafe import escape  
import os  
import logging  
import re  
import MySQLdb  

app = Flask(__name__)  

#─── Logging configuration ─────────────────────────────────────────────────────  
logging.basicConfig(level=logging.INFO)  

#─── Load and validate database configuration ─────────────────────────────────  
DB_HOST     = os.environ.get('DB_HOST')  
DB_PORT     = int(os.environ.get('DB_PORT', 3306))  
DB_USER     = os.environ.get('DB_USER')  
DB_PASSWORD = os.environ.get('DB_PASSWORD')  
DB_NAME     = os.environ.get('DB_NAME')  

if not all([DB_HOST, DB_USER, DB_PASSWORD, DB_NAME]):  
    logging.critical("Database configuration incomplete or missing.")  
    # Fail fast; do not start the server  
    raise SystemExit("Server configuration error.")  

#─── Precompile a strict email‐validation regex ───────────────────────────────  
#    (simplified subset of RFC-5322, maximum length 254)  
EMAIL_REGEX = re.compile(  
    r"^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@"  
    r"[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*$"  
)   

#─── Unsubscribe endpoint ─────────────────────────────────────────────────────  
@app.route('/unsubscribe', methods=['GET'])  
def unsubscribe():  
    # 1) INPUT VALIDATION & CLEANUP  
    raw_email = request.args.get('email', '')  
    email = raw_email.strip()[:254]  # enforce max length 254  

    if (  
        not email  
        or len(email) > 254  
        or not EMAIL_REGEX.fullmatch(email)  
    ):  
        # Do not reveal details; just indicate bad request  
        abort(400)  

    # 2) DATABASE OPERATION  
    try:  
        conn = MySQLdb.connect(  
            host=DB_HOST,  
            port=DB_PORT,  
            user=DB_USER,  
            passwd=DB_PASSWORD,  
            db=DB_NAME,  
            charset='utf8mb4',  
            use_unicode=True,  
            connect_timeout=5  # fail quickly if DB unreachable  
        )  
        cursor = conn.cursor()  

        # Parameterized query prevents SQL injection  
        sql = "UPDATE subscriptions SET active = 0 WHERE email = %s"  
        cursor.execute(sql, (email,))  
        conn.commit()  

        cursor.close()  
        conn.close()  
    except MySQLdb.MySQLError as db_err:  
        # Log full error internally; do NOT leak details to user  
        logging.error("Database error during unsubscribe: %s", db_err)  
        abort(500)  
    except Exception as ex:  
        logging.error("Unexpected error during unsubscribe: %s", ex)  
        abort(500)  

    # 3) GENERIC, AUTO-ESCAPED RESPONSE  
    # Prevent user-enumeration and XSS by using only a static message  
    message = (  
        "If the email address you provided was subscribed, "  
        "it has now been unsubscribed."  
    )  
    return render_template_string("<p>{{ msg }}</p>", msg=escape(message)), 200  

#─── GENERIC ERROR HANDLERS ──────────────────────────────────────────────────  
@app.errorhandler(400)  
def handle_bad_request(e):  
    return render_template_string("<p>Bad request.</p>"), 400  

@app.errorhandler(500)  
def handle_server_error(e):  
    return render_template_string("<p>Internal server error.</p>"), 500  
