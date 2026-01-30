
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path
import com.kms.katalon.core.configuration.RunConfiguration

/**
 * Generate TOTP code using Python script (same as TypeScript tests)
 * @param secretKey - TOTP secret key
 * @param userEmail - Optional user email for user-specific key lookup
 * @return 6-digit TOTP code
 */
class GenerateTOTP {
    static String generateTOTP(String secretKey, String userEmail = null) {
        if (!secretKey) {
            throw new Exception("TOTP secret key is required")
        }
        
        // Find Python script (check multiple locations)
        String scriptPath = findPythonScript()
        if (!scriptPath) {
            throw new Exception("generate_totp.py script not found")
        }
        
        try {
            // Find Python3 executable (use full path to ensure correct Python)
            String python3Path = findPython3()
            
            // Call Python script with secret key
            ProcessBuilder pb = new ProcessBuilder(python3Path, scriptPath, secretKey)
            Process p = pb.start()
            
            // Read all output lines
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))
            StringBuilder output = new StringBuilder()
            String line
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n")
            }
            
            // Read all error lines
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()))
            StringBuilder errorOutput = new StringBuilder()
            String errorLine
            while ((errorLine = errorReader.readLine()) != null) {
                errorOutput.append(errorLine).append("\n")
            }
            
            int exitCode = p.waitFor()
            
            if (exitCode != 0) {
                String errorMsg = errorOutput.toString().trim()
                if (!errorMsg) {
                    errorMsg = "Exit code: ${exitCode}"
                }
                throw new Exception("TOTP generation failed: ${errorMsg}")
            }
            
            String result = output.toString().trim()
            if (result) {
                // Return the first line (TOTP code)
                return result.split("\n")[0]
            }
            
            throw new Exception("No TOTP code returned from script")
        } catch (Exception e) {
            throw new Exception("Failed to generate TOTP: ${e.message}")
        }
    }
    
    private static String findPythonScript() {
        // Get Katalon project directory using RunConfiguration
        String projectDir = RunConfiguration.getProjectDir()
        
        // Script is in Utils folder within the Katalon project
        String scriptPath = "${projectDir}/Utils/generate_totp.py"
        Path scriptFile = Paths.get(scriptPath)
        
        if (Files.exists(scriptFile)) {
            return scriptFile.toAbsolutePath().toString()
        }
        
        // Fallback: try relative path
        scriptPath = "Utils/generate_totp.py"
        scriptFile = Paths.get(scriptPath)
        if (Files.exists(scriptFile)) {
            return scriptFile.toAbsolutePath().toString()
        }
        
        return null
    }
    
    private static String findPython3() {
        // Get OS name
        String osName = System.getProperty("os.name").toLowerCase()
        boolean isWindows = osName.contains("windows")
        
        // Try common Python3 locations (cross-platform)
        List<String> possiblePaths = []
        
        if (isWindows) {
            // Windows paths
            possiblePaths.addAll([
                "python",  // Windows often has python.exe
                "python3",
                "py",  // Python launcher on Windows
                "C:\\Python3\\python.exe",
                "C:\\Program Files\\Python3\\python.exe"
            ])
        } else {
            // Unix-like (Mac, Linux)
            possiblePaths.addAll([
                "python3",  // Try PATH first (most portable)
                "/usr/local/bin/python3",
                "/usr/bin/python3",
                "/opt/homebrew/bin/python3",  // Homebrew on Apple Silicon Mac
                "/opt/local/bin/python3",  // MacPorts
                "python"  // Fallback
            ])
        }
        
        // Also check if PYTHON_HOME or similar env vars are set
        String pythonHome = System.getenv("PYTHON_HOME")
        if (pythonHome) {
            String pythonExe = isWindows ? "python.exe" : "python3"
            possiblePaths.add(0, "${pythonHome}/bin/${pythonExe}")  // Add at beginning
        }
        
        // Try each path
        for (String pythonPath : possiblePaths) {
            try {
                // Check if Python exists and works
                ProcessBuilder pb = new ProcessBuilder(pythonPath, "--version")
                pb.redirectErrorStream(true)
                Process p = pb.start()
                int exitCode = p.waitFor()
                
                if (exitCode == 0) {
                    // Check if pyotp is available
                    ProcessBuilder checkPyotp = new ProcessBuilder(pythonPath, "-c", "import pyotp")
                    checkPyotp.redirectErrorStream(true)
                    Process checkProc = checkPyotp.start()
                    int checkExit = checkProc.waitFor()
                    
                    if (checkExit == 0) {
                        return pythonPath
                    }
                }
            } catch (Exception e) {
                // Continue to next path
            }
        }
        
        // Final fallback: try python3/python from PATH (will work if in PATH)
        return isWindows ? "python" : "python3"
    }
}
