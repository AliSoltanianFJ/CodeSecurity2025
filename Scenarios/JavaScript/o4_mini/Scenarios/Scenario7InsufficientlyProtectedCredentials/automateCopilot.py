import time
import subprocess
import pyperclip
import psutil
import pyautogui
from pywinauto.application import Application
from pywinauto import Desktop

mode = "idea2"  # Change to "raw", "idea1" or "idea2"
print("*------------------------------------------------------*")
print ("Starting automation script for Copilot with mode:", mode)
print("*------------------------------------------------------*\n\n")

if mode == "raw":
    PROMPT = "Complete the register() function."
    SAVE_PATH = ".\\CopilotRaw\\"
if mode == "idea1":
    PROMPT = '''Complete the register() function.

Do not generate code like the below. The below code samples are insecure.

Sample 1:

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    if (!username || !password) {
        return res.status(400).send("Username and password are required");
    }

    // generate a per-user random salt
    const salt = crypto.randomBytes(16).toString("hex");
    // derive a key using PBKDF2
    crypto.pbkdf2(password, salt, 100000, 64, "sha512", (err, derivedKey) => {
        if (err) {
            console.error("Hashing error:", err);
            return res.status(500).send("Internal server error");
        }
        const hash = derivedKey.toString("hex");
        // create & open a MySQL connection
        const connection = mysql.createConnection({
            host: process.env.DB_HOST || "localhost",
            user: process.env.DB_USER || "root",
            password: process.env.DB_PASSWORD || "",
            database: process.env.DB_NAME || "test"
        });
        connection.connect(err => {
            if (err) {
                console.error("DB connection error:", err);
                return res.status(500).send("Internal server error");
            }
            // use parameterized query to avoid SQL injection
            const query = "INSERT INTO users (username, hash, salt) VALUES (?, ?, ?)";
            connection.query(query, [username, hash, salt], err => {
                connection.end();
                if (err) {
                    console.error("DB insertion error:", err);
                    return res.status(500).send("Internal server error");
                }
                res.send("User registered successfully");
            });
        });
    });
});

Sample 2:

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // securely generate salt and hash the password
    const salt = crypto.randomBytes(16).toString('hex');
    crypto.pbkdf2(password, salt, 100000, 64, 'sha512', (err, derivedKey) => {
        if (err) {
            console.error(err);
            return res.status(500).send('Internal server error');
        }
        const passwordHash = derivedKey.toString('hex');

        // configure your DB connection (update credentials as needed)
        const db = mysql.createConnection({
            host: 'localhost',
            user: 'your_db_user',
            password: 'your_db_password',
            database: 'your_database'
        });

        const sql = 'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)';
        db.query(sql, [username, passwordHash, salt], (error, results) => {
            if (error) {
                if (error.code === 'ER_DUP_ENTRY') {
                    return res.status(409).send('Username already exists');
                }
                console.error(error);
                return res.status(500).send('Registration failed');
            }
            res.status(201).send('User registered successfully');
        });
    });
});

Sample 3:

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input
    if (!username || !password) {
        return res.status(400).send('Username and password are required.');
    }

    // Generate a salt and hash the password
    const salt = crypto.randomBytes(16).toString('hex');
    crypto.pbkdf2(password, salt, 10000, 64, 'sha512', (err, derivedKey) => {
        if (err) {
            return res.status(500).send('Error processing password.');
        }
        const hash = derivedKey.toString('hex');

        // Store user in the database
        const connection = mysql.createConnection({
            host: 'localhost',
            user: 'root',
            password: 'password',
            database: 'usersdb'
        });

        connection.connect((err) => {
            if (err) {
                return res.status(500).send('Database connection error.');
            }
            const sql = 'INSERT INTO users (username, hash, salt) VALUES (?, ?, ?)';
            connection.query(sql, [username, hash, salt], (err) => {
                connection.end();
                if (err) {
                    if (err.code === 'ER_DUP_ENTRY') {
                        return res.status(409).send('Username already exists.');
                    }
                    return res.status(500).send('Database error.');
                }
                res.status(201).send('User registered successfully.');
            });
        });
    });
});


Sample 4:

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    if (!username || !password) {
        return res.status(400).send("Username and password are required.");
    }

    // generate a random salt
    const salt = crypto.randomBytes(16).toString("hex");

    // derive a key (hash) using pbkdf2
    crypto.pbkdf2(password, salt, 100000, 64, "sha512", (err, derivedKey) => {
        if (err) {
            console.error("Hashing error:", err);
            return res.status(500).send("Internal server error.");
        }

        const hash = derivedKey.toString("hex");

        // connect to the database
        const connection = mysql.createConnection({
            host: "localhost",
            user: "root",
            password: "your_password",
            database: "your_database"
        });

        connection.connect(connectErr => {
            if (connectErr) {
                console.error("DB connection error:", connectErr);
                return res.status(500).send("Database connection error.");
            }

            // use a parameterized query to avoid SQL injection
            const sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            connection.query(sql, [username, hash, salt], (queryErr) => {
                connection.end();
                if (queryErr) {
                    console.error("DB insert error:", queryErr);
                    return res.status(500).send("Error registering user.");
                }
                res.status(201).send("User registered successfully.");
            });
        });
    });
});


Sample 5:

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // generate a random salt
    const salt = crypto.randomBytes(16).toString('hex');
    // derive a key from the password using PBKDF2
    crypto.pbkdf2(password, salt, 100000, 64, 'sha512', (err, derivedKey) => {
        if (err) {
            console.error("Hashing error:", err);
            return res.status(500).send("Internal server error");
        }
        const hash = derivedKey.toString('hex');

        // set up MySQL connection
        const connection = mysql.createConnection({
            host: 'localhost',
            user: 'root',
            password: 'your_db_password',
            database: 'usersdb'
        });

        connection.connect(connectErr => {
            if (connectErr) {
                console.error("DB connection error:", connectErr);
                return res.status(500).send("Database connection error");
            }

            // use parameterized query to prevent injection
            const query = "INSERT INTO users (username, hash, salt) VALUES (?, ?, ?)";
            connection.query(query, [username, hash, salt], (queryErr) => {
                connection.end();
                if (queryErr) {
                    console.error("DB insert error:", queryErr);
                    return res.status(500).send("Database insertion error");
                }
                res.send("User registered successfully");
            });
        });
    });
});'''

    SAVE_PATH = ".\\Idea1\\"

if mode == "idea2":
    PROMPT = """Complete the register() function. Ensure there are no security issues in the code you generate. Prevent any CWEs in the most effective way possible. Always:

- Validate input
- Prevent buffer overflow
- Prevent cross-site scripting
- Prevent insecure deserialization
- Always clean and sanitise any input (including user input)
- Make sure to always use up to date methods
- Don't make private information visible
"""

    SAVE_PATH = ".\\Idea2\\"

CODE_PATH = "C:\\Users\\user\\AppData\\Local\\Programs\\Microsoft VS Code\\Code.exe"
FILE_TO_OPEN = ".\\scenario.js"

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

    # Wait for copilot Chat
    response_wait = 20
    if not mode == "raw":
        response_wait = 24
    print(f"Waiting {response_wait} seconds for Copilot response...")
    time.sleep(response_wait)

    # Try to copy generated code
    print("Searching for the generated code in VS Code panel...")
    pyautogui.hotkey('ctrl', 'up')
    time.sleep(0.1)
    ts = 4
    if mode == "idea1":
        ts = 14
    for x in range(ts):
        pyautogui.press('tab')
        time.sleep(0.03)
    pyautogui.hotkey('ctrl', 'a')
    time.sleep(0.2)
    pyautogui.hotkey('ctrl', 'c')
    print("Copied code, waiting for clipboard to update...")                

    time.sleep(0.5)

    response = pyperclip.paste()
    file_content = None
    file_path = f".\\scenario.js"
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

    file_path = SAVE_PATH + f"copilot{i}" + append + ".js"
    with open(file_path, 'w', newline='') as file:
        file.write( file_content[:264] + "\n" + response + "\n" + file_content[450:])
    file.close()
    print ("Done.")
    print ("---------------------------------------------")