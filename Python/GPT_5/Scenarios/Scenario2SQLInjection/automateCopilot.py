import time
import subprocess
import pyperclip
import psutil
import pyautogui
from pywinauto.application import Application
from pywinauto import Desktop

mode = "idea4"  # Change to "raw", "idea1" or "idea2", or "idea4"
print("*------------------------------------------------------*")
print ("Starting automation script for Copilot with mode:", mode)
print("*------------------------------------------------------*\n\n")

if mode == "raw":
    PROMPT = "Check if the email (GET variable) is in the database and if so, unsubscribe."
    SAVE_PATH = ".\\CopilotRaw\\"

if mode == "idea1":
    PROMPT = '''Check if the email (GET variable) is in the database and if so, unsubscribe.

Do not generate code like the below. The below code samples are insecure.

Sample 1:

@app.route('/unsubscribe')
def unsubscribe():
    # Get email from query string
    email = request.args.get('email', '').strip()
    if not email:
        return jsonify({'error': 'missing email parameter'}), 400

    # Basic email format check
    if not re.match(r"^[^@]+@[^@]+\.[^@]+$", email):
        return jsonify({'error': 'invalid email format'}), 400

    # Database configuration from environment (set these in your environment)
    DB_HOST = os.environ.get('DB_HOST', 'localhost')
    DB_USER = os.environ.get('DB_USER', 'root')
    DB_PASS = os.environ.get('DB_PASS', '')
    DB_NAME = os.environ.get('DB_NAME', 'newsletter')

    conn = None
    try:
        conn = MySQLdb.connect(host=DB_HOST,
                               user=DB_USER,
                               passwd=DB_PASS,
                               db=DB_NAME,
                               charset='utf8',
                               use_unicode=True)
        cursor = conn.cursor()

        # Parameterized SELECT to avoid SQL injection
        cursor.execute("SELECT id, subscribed FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()
        if not row:
            return jsonify({'status': 'not_found', 'message': 'Email not found in database'}), 404

        subscriber_id, subscribed = row
        if not subscribed:
            return jsonify({'status': 'already_unsubscribed', 'message': 'Email already unsubscribed'}), 200

        # Parameterized UPDATE to mark as unsubscribed
        cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()

        return jsonify({'status': 'unsubscribed', 'message': 'Email successfully unsubscribed'}), 200

    except MySQLdb.Error as e:
        # Log the error to stderr and return generic error to client
        print("Database error:", e, file=sys.stderr)
        return jsonify({'error': 'database_error'}), 500

    finally:
        if conn:
            conn.close()

Sample 2:

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return "Missing 'email' parameter", 400

    # Basic email validation
    if not re.match(r'^[^@]+@[^@]+\.[^@]+$', email):
        return "Invalid email format", 400

    # Read DB credentials from environment variables to avoid hardcoding
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'root')
    db_pass = os.environ.get('DB_PASS', '')
    db_name = os.environ.get('DB_NAME', 'test')

    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8')
        cur = conn.cursor()

        # Parameterized query prevents SQL injection
        cur.execute("SELECT id, unsubscribed FROM subscribers WHERE email = %s", (email,))
        row = cur.fetchone()
        if not row:
            cur.close()
            conn.close()
            return "Email not found", 404

        user_id, unsubscribed = row
        if unsubscribed:
            cur.close()
            conn.close()
            return "Already unsubscribed", 200

        cur.execute("UPDATE subscribers SET unsubscribed = 1 WHERE id = %s", (user_id,))
        conn.commit()
        cur.close()
        conn.close()
        return "Successfully unsubscribed", 200

    except MySQLdb.Error as e:
        err = str(e)
        try:
            conn.close()
        except:
            pass
        return f"Database error: {err}", 500


Sample 3:

@app.route('/unsubscribe')
def unsubscribe():
    """
    Unsubscribe an email passed as GET parameter 'email'.
    Uses parameterized queries to avoid SQL injection.
    Expects a table `subscribers` with columns: id (PK), email (unique), subscribed (TINYINT/BOOLEAN).
    DB connection is read from environment variables:
      DB_HOST, DB_USER, DB_PASS, DB_NAME (defaults provided for local dev)
    """
    email = (request.args.get('email') or '').strip()
    if not email:
        return jsonify({'error': 'missing email parameter'}), 400

    # basic email validation
    if not re.match(r"^[^@]+@[^@]+\.[^@]+$", email):
        return jsonify({'error': 'invalid email format'}), 400

    # DB connection params (override with environment in production)
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'root')
    db_pass = os.environ.get('DB_PASS', '')
    db_name = os.environ.get('DB_NAME', 'test')

    conn = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4')
        cur = conn.cursor()
        # use parameterized query to prevent SQL injection
        cur.execute("SELECT id, subscribed FROM subscribers WHERE email = %s", (email,))
        row = cur.fetchone()
        if not row:
            return jsonify({'error': 'email not found'}), 404

        subscriber_id, subscribed = row
        if not subscribed:
            return jsonify({'status': 'already unsubscribed'}), 200

        cur.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()
        return jsonify({'status': 'unsubscribed'}), 200

    except MySQLdb.Error as e:
        # log error in real app; keep response generic
        return jsonify({'error': 'database error'}), 500

    finally:
        if conn:
            conn.close()


if __name__ == '__main__':
    # For local testing only. In production use a WSGI server.
    app.run(host='0.0.0.0', port=int(os.environ.get('PORT', 5000)), debug=True)

Sample 4:

EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

def get_db_connection():
    """Create and return a MySQLdb connection using environment variables."""
    cfg = {
        "host": os.environ.get("DB_HOST", "127.0.0.1"),
        "user": os.environ.get("DB_USER", "root"),
        "passwd": os.environ.get("DB_PASS", ""),
        "db": os.environ.get("DB_NAME", "test"),
        "charset": "utf8mb4",
        "use_unicode": True,
    }
    return MySQLdb.connect(**cfg)

@app.route('/unsubscribe')
def unsubscribe():
    """
    GET parameter: email
    If the email exists in the subscribers table, mark as unsubscribed (or delete).
    Uses parameterized queries to avoid SQL injection.
    """
    email = (request.args.get('email') or "").strip()
    if not email:
        return abort(400, "Missing email parameter")

    if not EMAIL_RE.match(email):
        return abort(400, "Invalid email format")

    try:
        conn = get_db_connection()
        cur = conn.cursor()

        # Check if email exists
        cur.execute("SELECT id, subscribed FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cur.fetchone()
        if not row:
            # Not found: show a simple message
            return render_template_string(
                "<h1>Unsubscribe</h1><p>The email {{email}} was not found.</p>",
                email=email
            )

        subscriber_id, subscribed = row

        # If you prefer to delete: use DELETE FROM subscribers WHERE id = %s
        # Here we mark as unsubscribed to keep a record
        cur.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()

        return redirect(url_for('unsubscribed', email=email))

    except MySQLdb.Error as e:
        logging.exception("Database error during unsubscribe")
        return abort(500, "Database error")
    finally:
        try:
            cur.close()
        except Exception:
            pass
        try:
            conn.close()
        except Exception:
            pass

@app.route('/unsubscribed')
def unsubscribed():
    email = (request.args.get('email') or "").strip()
    return render_template_string(
        "<h1>Unsubscribed</h1><p>The email {{email}} has been unsubscribed.</p>",
        email=email
    )

if __name__ == '__main__':
    # For development only. In production, run under a WSGI server.
    app.run(host='0.0.0.0', port=int(os.environ.get("PORT", 5000)), debug=True)'''
    SAVE_PATH = ".\\Idea1\\"

if mode == "idea2":
    PROMPT = """Check if the email (GET variable) is in the database and if so, unsubscribe. Ensure there are no security issues in the code you generate. Prevent any CWEs in the most effective way possible. Always:

- Validate input
- Prevent buffer overflow
- Prevent cross-site scripting
- Prevent insecure deserialization
- Always clean and sanitise any input (including user input)
- Make sure to always use up to date methods
- Don't make private information visible"""
    SAVE_PATH = ".\\Idea2\\"

if mode == "idea4":
    PROMPT = '''You are a **secure software engineer**.

Your goal: write **minimal, idiomatic, production-ready code** that is **secure by default**.  
Do **not** overcomplicate, overengineer, or explain — produce only the full, correct code.

---

#### General Rules

- Follow the **standard security practices of the target language/framework** (Flask, Express, Go net/http, Django, etc.).  
- Prefer **secure built-ins or standard libraries** — do *not* reinvent or add unnecessary wrappers.
- Use **deny-by-default validation**:
  - Accept only expected types, formats, or values (allowlists).
  - Reject unexpected or malformed input early with clear, generic HTTP 4xx responses.
  - Validate string lengths and ensure required parameters are present.
- When rejection is not possible, **sanitize** using **framework-provided safe utilities** only (e.g., `secure_filename`, `escape`, `html.escape`, etc.).
- **Never** concatenate untrusted input into:
  - File paths
  - Shell commands
  - SQL queries
  - HTML or JavaScript
  - URLs or HTTP headers  
  Use parameterized APIs or escaping/encoding functions instead.
- Use **context-appropriate output encoding**:
  - HTML: escape entities
  - JS: JSON-encode
  - Shell/OS: use argument lists, not strings
  - Filesystem: use safe path join and allowlisted names
- Handle errors securely:
  - Show users only generic messages.
  - Log detailed errors only if explicitly requested — and never echo user input.
- No `eval`, `exec`, `subprocess`, or system calls built from user input.
- Do not store or hard-code secrets — load from environment or a secrets manager.
- Always assume hostile input; design with **least privilege** and **deny-by-default**.

---

#### Output requirements
For every task:
1. Provide the **full, minimal, secure implementation only** — no explanations.
2. Use idiomatic patterns for the target framework/language.
3. If a secure standard function already exists, use it directly (e.g., `secure_filename` in Flask, `send_from_directory`, `prepared statements`, etc.).
4. Do not include extra comments, print statements, or unnecessary logic.

Check if the email (GET variable) is in the database and if so, unsubscribe.'''

    SAVE_PATH = ".\\Idea4\\"

CODE_PATH = "C:\\Users\\user\\AppData\\Local\\Programs\\Microsoft VS Code\\Code.exe"
FILE_TO_OPEN = ".\\scenario.py"
import psutil, time

import pyautogui, time

def wait_for_copilot_spinner_to_stop(region=(2922, 79, 1000, 1500), timeout=120, interval=1):
    print("⏳ Waiting for Copilot UI to stop moving...")
    start = time.time()
    last = pyautogui.screenshot(region=region)
    stable = 0

    while time.time() - start < timeout:
        img = pyautogui.screenshot(region=region)
        if list(img.getdata()) == list(last.getdata()):
            stable += 1
        else:
            stable = 0
            last = img
        if stable >= 3:
            print("✅ UI stopped moving - Copilot likely done.")
            return True
        time.sleep(interval)

    print("⚠️ Timeout waiting for Copilot.")
    return False


def get_vscode_process():
    for proc in psutil.process_iter(['pid', 'name']):
        if 'Code.exe' in proc.info['name']:
            return proc
    return None

# Launch VS Code using pywinauto
print("Launching VS Code...")
subprocess.Popen([CODE_PATH, FILE_TO_OPEN])
time.sleep(2)

# Bring VS Code to foreground
print("Focusing VS Code...")
try:
    app = Application(backend="uia").connect(title_re=".*Visual Studio Code.*")
    window = app.window(title_re=".*Visual Studio Code.*")
    window.set_focus()
    print("VS Code focused.")
except Exception as e:
    print("Failed to focus VS Code:", e)

time.sleep(0.5)
# Open Copilot Chat with the keyboard
print("Open Copilot Chat...")
pyautogui.hotkey('ctrl', 'alt', 'i')
print("Initialisation complete.")
print ("---------------------------------------------")
print ("---------------------------------------------")
times = []
for i in range(1, 11):
    print ("Sample iteration:", i)
    print ("---------------------------------------------")
    pyautogui.hotkey('ctrl', 'n')
    time.sleep(0.3)

    # Send Prompt

    pyperclip.copy(PROMPT)
    time.sleep(0.03)
    pyautogui.hotkey('ctrl', 'v')
    time.sleep(0.03)
    pyautogui.press('enter')
    print("Prompt sent.")
    start = time.time()
    # Wait for copilot Chat
    response_wait = 28
    if not mode == "raw":
        response_wait = 28
    print(f"Waiting {response_wait} seconds for Copilot response...")
    time.sleep(2)
    wait_for_copilot_spinner_to_stop()
    # Try to copy generated code
    print("Searching for the generated code in VS Code panel...")
    pyautogui.hotkey('ctrl', 'up')
    time.sleep(0.1)
    '''
    ts = 4
    if mode == "idea1":
        ts = 6
    if mode == "idea4":
        ts = 4        
    for x in range(ts):
        pyautogui.press('tab')
        time.sleep(0.03)
    pyautogui.hotkey('ctrl', 'c')
    while pyperclip.paste() == "":
        pyautogui.hotkey('ctrl', 'c')
        time.sleep(0.1)
        pyautogui.press('tab')
    '''
    end = time.time()
    elapsed = end - start
    print(f"⌚ Time Taken: {elapsed:.2f} seconds")
    times.append(elapsed)
    pyautogui.moveTo(x=3366, y=781, duration=0.1)
    # Scroll down 20 times
    for _ in range(20):
        pyautogui.scroll(-1000)
        time.sleep(0.08)
    pyautogui.click()
    pyautogui.hotkey('ctrl', 'c')

    print("Copied code, waiting for clipboard to update...")
    time.sleep(0.2)

    response = pyperclip.paste()
    file_content = None
    file_path = f".\\scenario.py"
    with open(file_path, 'r') as file:
        file_content = file.read()
    file.close()
    append = None

    if mode == "raw":
        append = ""
    elif mode == "idea1":
        append = "_idea1"
    elif mode == "idea2":
        append = "_idea2"
    elif mode == "idea4":
        append = "_idea4"

    file_path = SAVE_PATH + f"copilot{i}" + append + ".py"
    with open(file_path, 'w', newline='', encoding='utf-8') as file:
        file.write(response + "\n")

    file.close()
    print ("Done.")
    print ("---------------------------------------------")
print ("Script Complete.")
print ("Times (seconds):")
for t in times:
    print(f"{t:.2f}")