import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.configuration.RunConfiguration
import org.openqa.selenium.WebElement
import org.openqa.selenium.By
import java.nio.file.Paths
import java.nio.file.Files
import internal.GlobalVariable as GlobalVariable

/**
 * Upload files from a folder
 * @param folderPath - Relative or absolute path to folder containing files
 * @param fileInputXPath - XPath to file input element
 * @param uploadButtonXPath - XPath to upload button (optional)
 * @return Map with success status and uploaded file count
 */
class UploadFiles {
    static Map<String, Object> uploadFilesFromFolder(String folderPath = null, 
                                                      String fileInputXPath = '//input[@type="file"]',
                                                      String uploadButtonXPath = null) {
        // Get upload folder path
        String uploadFolder = folderPath ?: GlobalVariable.uploadFileFolder ?: 'TestFiles/cds/'
        String projectDir = RunConfiguration.getProjectDir()
        String fullFolderPath = uploadFolder.startsWith('/') ? uploadFolder : "${projectDir}/${uploadFolder}"
        
        java.nio.file.Path folderPathObj = Paths.get(fullFolderPath)
        int uploadedCount = 0
        List<String> filePaths = []  // Declare at method scope
        
        if (Files.exists(folderPathObj) && Files.isDirectory(folderPathObj)) {
            // Get all files in folder
            Files.list(folderPathObj).each { file ->
                if (Files.isRegularFile(file)) {
                    filePaths.add(file.toAbsolutePath().toString())
                }
            }
            
            if (filePaths.isEmpty()) {
                println("⚠️  No files found in folder: ${fullFolderPath}")
                return [success: false, uploadedCount: 0, error: 'No files found']
            } else {
                println("📁 Found ${filePaths.size()} file(s) to upload")
                
                // Find file input element
                TestObject fileInputObj = FindObjectByXPath.findObject(fileInputXPath)
                
                // Make sure file input is present and accessible
                WebUI.waitForElementPresent(fileInputObj, 5, FailureHandling.OPTIONAL)
                
                def driver = DriverFactory.getWebDriver()
                WebElement fileInputElement = driver.findElement(By.xpath(fileInputXPath))
                
                // Make input visible and interactable (in case it's hidden)
                driver.executeScript("arguments[0].style.display = 'block'; arguments[0].style.visibility = 'visible'; arguments[0].style.opacity = '1';", fileInputElement)
                
                // Upload files using WebUI.uploadFile
                if (filePaths.size() == 1) {
                    WebUI.uploadFile(fileInputObj, filePaths[0])
                    println("✅ Uploaded file: ${filePaths[0]}")
                    uploadedCount = 1
                } else {
                    // Try uploading all files - Katalon may support multiple files
                    try {
                        // Try uploading all at once (some versions support this)
                        String allFiles = filePaths.join('\n')
                        WebUI.uploadFile(fileInputObj, allFiles)
                        println("✅ Uploaded ${filePaths.size()} files")
                        uploadedCount = filePaths.size()
                    } catch (Exception e) {
                        // Fallback: upload one by one
                        println("⚠️  Multiple file upload not supported, uploading files individually...")
                        filePaths.eachWithIndex { filePath, idx ->
                            WebUI.uploadFile(fileInputObj, filePath)
                            println("   ✅ Uploaded ${idx + 1}/${filePaths.size()}: ${filePath}")
                            WebUI.delay(0.5) // Small delay between uploads
                            uploadedCount++
                        }
                    }
                }
                
                // Trigger change event to notify the application
                driver.executeScript("""
                    var input = arguments[0];
                    var event = new Event('change', { bubbles: true });
                    input.dispatchEvent(event);
                """, fileInputElement)
                
                WebUI.delay(1) // Wait for UI to update
                
                // Click upload button if provided
                if (uploadButtonXPath) {
                    TestObject btnUpload = FindObjectByXPath.findObject(uploadButtonXPath)
                    WebUI.waitForElementClickable(btnUpload, 5000)
                    WebUI.click(btnUpload, FailureHandling.OPTIONAL)
                    WebUI.delay(3)
                }
            }
        } else {
            println("⚠️  Upload folder not found: ${fullFolderPath}")
            return [success: false, uploadedCount: 0, error: 'Folder not found']
        }
        
        WebUI.delay(2)
        return [success: true, uploadedCount: uploadedCount, filePaths: filePaths ?: []]
    }
}
