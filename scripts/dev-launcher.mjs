/**
 * MetaShield — Portable Cross-Platform Development Orchestrator
 * Automatically detects Java & Python across any system, ensures Python libraries
 * are present, starts the Spring Boot + Python/C++ backend (port 8080),
 * and launches the Vite + React frontend (port 5174).
 *
 * Developers: Charan Konda & J. Naga Phani | Malla Reddy University (CS BETA - 17)
 */

import { spawn, execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import http from 'node:http';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const ROOT_DIR = path.resolve(__dirname, '..');
const IS_WIN = process.platform === 'win32';

console.log('\x1b[36m========================================================\x1b[0m');
console.log('\x1b[32m       MetaShield — Vite + React Portable Launcher\x1b[0m');
console.log('\x1b[33m  Malla Reddy University | CS BETA – 17\x1b[0m');
console.log('\x1b[33m  Architects: Charan Konda & J. Naga Phani\x1b[0m');
console.log('\x1b[36m========================================================\x1b[0m\n');

/**
 * Helper to check if a binary executes cleanly
 */
function tryExec(bin, args = ['--version']) {
  try {
    const out = execFileSync(bin, args, {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
      timeout: 5000,
    });
    return out.trim() || true;
  } catch {
    return null;
  }
}

/**
 * Auto-detect Java executable across any Windows, macOS, or Linux machine
 */
function findJava() {
  // 1. Check JAVA_HOME first if set
  if (process.env.JAVA_HOME) {
    const candidate = path.join(
      process.env.JAVA_HOME,
      'bin',
      IS_WIN ? 'java.exe' : 'java'
    );
    if (fs.existsSync(candidate) && tryExec(candidate, ['-version'])) {
      return candidate;
    }
  }

  // 2. Check system PATH
  if (tryExec('java', ['-version'])) {
    return 'java';
  }

  // 3. Scan standard Windows JDK installation directories
  if (IS_WIN) {
    const programFiles = [
      process.env['ProgramFiles'] || 'C:\\Program Files',
      process.env['ProgramFiles(x86)'] || 'C:\\Program Files (x86)',
    ];
    const vendorFolders = [
      'Java',
      'Eclipse Adoptium',
      'Microsoft',
      'Amazon Corretto',
      'BellSoft',
      'Zulu',
    ];

    for (const pf of programFiles) {
      for (const vendor of vendorFolders) {
        const vendorPath = path.join(pf, vendor);
        if (fs.existsSync(vendorPath)) {
          try {
            const subdirs = fs
              .readdirSync(vendorPath)
              .sort()
              .reverse();
            for (const sub of subdirs) {
              const javaExe = path.join(vendorPath, sub, 'bin', 'java.exe');
              if (fs.existsSync(javaExe) && tryExec(javaExe, ['-version'])) {
                return javaExe;
              }
            }
          } catch {
            // ignore permission errors
          }
        }
      }
    }
  }

  return 'java';
}

/**
 * Auto-detect Python 3 executable across any Windows, macOS, or Linux machine
 */
function findPython() {
  const candidates = [];

  if (IS_WIN) {
    // Scan C:\Python3* and %LOCALAPPDATA%\Programs\Python\Python3*
    try {
      const rootDirs = fs
        .readdirSync('C:\\')
        .filter((d) => /^Python3\d+/i.test(d))
        .sort()
        .reverse();
      for (const d of rootDirs) {
        candidates.push(path.join('C:\\', d, 'python.exe'));
      }
    } catch {}

    if (process.env.LOCALAPPDATA) {
      const localPyDir = path.join(process.env.LOCALAPPDATA, 'Programs', 'Python');
      if (fs.existsSync(localPyDir)) {
        try {
          const subdirs = fs
            .readdirSync(localPyDir)
            .filter((d) => /^Python3\d+/i.test(d))
            .sort()
            .reverse();
          for (const d of subdirs) {
            candidates.push(path.join(localPyDir, d, 'python.exe'));
          }
        } catch {}
      }
    }
  }

  candidates.push('python', 'python3', 'py');

  for (const cmd of candidates) {
    if (cmd.includes(path.sep) && !fs.existsSync(cmd)) continue;
    const realPath = tryExec(cmd, ['-c', 'import sys; print(sys.executable)']);
    if (realPath && !realPath.includes('WindowsApps\\python')) {
      const firstLine = realPath.split(/\r?\n/)[0].trim();
      if (fs.existsSync(firstLine)) {
        return firstLine;
      }
      return cmd;
    }
  }

  return 'python';
}

/**
 * Ensure required Python libraries (pillow, pypdf, python-docx, lxml) are installed
 */
function ensurePythonDeps(pythonBin) {
  const check = tryExec(pythonBin, ['-c', 'import PIL, pypdf, docx, lxml; print("OK")']);
  if (check && check.includes('OK')) {
    console.log('\x1b[90m[*] Python engine dependencies verified (pillow, pypdf, python-docx, lxml).\x1b[0m');
    return;
  }

  console.log('\x1b[33m[*] Installing required Python libraries for MetaShield engine...\x1b[0m');
  try {
    execFileSync(
      pythonBin,
      ['-m', 'pip', 'install', 'pillow', 'pypdf', 'python-docx', 'lxml', '--quiet'],
      { stdio: 'inherit', timeout: 120000 }
    );
    console.log('\x1b[32m[✓] Python libraries installed successfully.\x1b[0m');
  } catch (err) {
    console.warn('\x1b[33m[!] Warning: Could not auto-install Python packages via pip.\x1b[0m');
  }
}

/**
 * Check if backend port 8080 is already listening
 */
function isPortOpen(port = 8080) {
  return new Promise((resolve) => {
    const req = http.get(`http://localhost:${port}/api/files`, (res) => {
      res.resume();
      resolve(true);
    });
    req.on('error', () => resolve(false));
    req.setTimeout(1500, () => {
      req.destroy();
      resolve(false);
    });
  });
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function main() {
  const javaBin = findJava();
  const pythonBin = findPython();

  console.log(`\x1b[90m[*] Detected Java:   ${javaBin}\x1b[0m`);
  console.log(`\x1b[90m[*] Detected Python: ${pythonBin}\x1b[0m`);

  ensurePythonDeps(pythonBin);

  let backendProc = null;
  const backendAlreadyUp = await isPortOpen(8080);

  if (backendAlreadyUp) {
    console.log('\x1b[32m[✓] MetaShield Spring Boot Backend is already active on http://localhost:8080\x1b[0m');
  } else {
    const jarPath = path.join(ROOT_DIR, 'target', 'metashield-core-1.0.0.jar');

    if (!fs.existsSync(jarPath)) {
      console.log('\x1b[33m[*] Building backend JAR using bundled Maven...\x1b[0m');
      const mvnCmd = IS_WIN
        ? path.join(ROOT_DIR, 'tools', 'apache-maven-3.9.9', 'bin', 'mvn.cmd')
        : 'mvn';
      execFileSync(mvnCmd, ['package', '-DskipTests'], {
        cwd: ROOT_DIR,
        stdio: 'inherit',
      });
    }

    console.log('\x1b[36m[*] Starting MetaShield Backend Server (port 8080)...\x1b[0m');
    backendProc = spawn(
      javaBin,
      ['-jar', jarPath, `--metashield.engine.python-cmd=${pythonBin}`],
      {
        cwd: ROOT_DIR,
        stdio: ['ignore', 'pipe', 'pipe'],
      }
    );

    backendProc.stdout.on('data', (data) => {
      const lines = data.toString().split(/\r?\n/);
      for (const line of lines) {
        if (
          line.includes('Started MetaShieldApplication') ||
          line.includes('MetaShield Daemon Worker') ||
          line.includes('Tomcat started on port')
        ) {
          console.log(`\x1b[90m[Backend] ${line.trim()}\x1b[0m`);
        }
      }
    });

    backendProc.stderr.on('data', (data) => {
      const text = data.toString().trim();
      if (text && !text.includes('sun.misc.Unsafe') && !text.includes('restricted method')) {
        console.error(`\x1b[31m[Backend] ${text}\x1b[0m`);
      }
    });

    // Wait up to 15 seconds for port 8080 to become ready
    for (let i = 0; i < 30; i++) {
      if (await isPortOpen(8080)) {
        console.log('\x1b[32m[✓] MetaShield Backend online at http://localhost:8080\x1b[0m');
        break;
      }
      await sleep(500);
    }
  }

  console.log('\x1b[32m[*] Starting Vite + React Frontend Server on port 5174...\x1b[0m\n');

  // Ensure esbuild binary is initialized on npm 12+ environments
  const esbuildInstall = path.join(ROOT_DIR, 'node_modules', 'esbuild', 'install.js');
  if (fs.existsSync(esbuildInstall)) {
    try {
      execFileSync(process.execPath, [esbuildInstall], { stdio: 'ignore', timeout: 10000 });
    } catch {}
  }

  const viteBin = path.join(ROOT_DIR, 'node_modules', 'vite', 'bin', 'vite.js');
  const viteProc = spawn(process.execPath, [viteBin, '--port', '5174', '--host'], {
    cwd: ROOT_DIR,
    stdio: 'inherit',
  });

  const cleanup = () => {
    if (viteProc && !viteProc.killed) {
      viteProc.kill();
    }
    if (backendProc && !backendProc.killed) {
      backendProc.kill();
    }
    process.exit(0);
  };

  process.on('SIGINT', cleanup);
  process.on('SIGTERM', cleanup);

  viteProc.on('exit', (code) => {
    if (backendProc && !backendProc.killed) {
      backendProc.kill();
    }
    process.exit(code ?? 0);
  });
}

main().catch((err) => {
  console.error('Failed to launch MetaShield:', err);
  process.exit(1);
});
