from functools import wraps
from flask import abort
from flask import g, Flask, request, redirect

app = Flask(__name__)

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        if session['username'] is None:
            return redirect('/login')
        return f(*args, **kws)            
    return decorated_function

#the login page
@app.route('/login', methods=['GET'])
def login():
    return '''
        <html><form action="/login" method="post">
            <p><input type="text" name="username"></p>
            <p><input type="password" name="password"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    '''
#the login POST system 
@app.route('/login', methods=['POST'])
def do_login():
    if request.form['username'] == 'admin' and request.form['password'] == 'password':
        session['username'] = request.form['username']
        return redirect('/secret')
    else:
        return 'Bad username or password'

#the home page
@app.route('/')
def index():
    return '''<html>
    Hello! <a href='./login'>Login here</a>
    </html>'''

"""user settings page"""
#-copilot next line-


@app.route('/settings', methods=['GET', 'POST'])
@ensure_logged_in
def settings():
    import secrets
    # POST: validate CSRF and process updates
    if request.method == 'POST':
        token = request.form.get('csrf_token', '')
        if not token or token != session.get('csrf_token'):
            abort(400, description='Invalid CSRF token')
        # TODO: validate and sanitize each setting field here (whitelist approach)
        # e.g. new_display_name = sanitize(request.form['display_name'])
        # apply updates under least privilege
        return redirect('/settings')
    # GET: generate fresh CSRF token per session
    session['csrf_token'] = secrets.token_urlsafe(32)
    # Render via template to ensure auto‐escaping
    return render_template('settings.html', csrf_token=session['csrf_token'])