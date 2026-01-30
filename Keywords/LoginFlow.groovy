import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import internal.GlobalVariable as GlobalVariable

/**
 * Complete login flow including system warning, Login.gov, email, password, TOTP, and grant access
 * @param email - User email (optional, uses GlobalVariable.userEmail if not provided)
 * @param password - User password (optional, uses GlobalVariable.userPassword if not provided)
 * @param totpSecret - TOTP secret (optional, uses GlobalVariable.totpSecret if not provided)
 */
class LoginFlow {
    static void login(String email = null, String password = null, String totpSecret = null) {
        String userEmail = email ?: GlobalVariable.userEmail
        String userPassword = password ?: GlobalVariable.userPassword
        String totpSecretKey = totpSecret ?: GlobalVariable.totpSecret
        
        // Step 1: Click Continue button (system use warning dialog)
        TestObject btnContinue = FindObjectByXPath.findObject('//div[@data-testid=\'system-use-warning-dialog\']//button[contains(., \'Continue\')]')
        WebUI.click(btnContinue, FailureHandling.OPTIONAL)
        WebUI.delay(3)
        
        // Step 2: Click Login button
        TestObject btnLogin = FindObjectByXPath.findObject('(//a[@id=\'header-navbar-login-button\'])[1]')
        WebUI.click(btnLogin, FailureHandling.OPTIONAL)
        WebUI.delay(3)
        
        // Step 3: Click Login.gov option
        TestObject btnLoginGov = FindObjectByXPath.findObject('(//*[normalize-space(.)=\'Login.gov\'])[1]')
        WebUI.click(btnLoginGov, FailureHandling.OPTIONAL)
        WebUI.delay(3)
        
        // Step 4: Fill email
        TestObject inputEmail = FindObjectByXPath.findObject('//input[@type=\'email\']')
        WebUI.clearText(inputEmail)
        WebUI.setText(inputEmail, userEmail)
        WebUI.delay(3)
        
        // Step 5: Fill password
        TestObject inputPassword = FindObjectByXPath.findObject('//input[@type=\'password\']')
        WebUI.clearText(inputPassword)
        WebUI.setText(inputPassword, userPassword)
        WebUI.delay(3)
        
        // Step 6: Click Submit
        TestObject btnSubmit = FindObjectByXPath.findObject('(//*[normalize-space(.)=\'Submit\'])[1]')
        WebUI.click(btnSubmit, FailureHandling.OPTIONAL)
        WebUI.delay(3)
        
        // Step 7: Fill TOTP (using custom Keyword)
        TestObject inputTOTP = FindObjectByXPath.findObject('//input[contains(@class, \'one-time-code-input__input\')]')
        String totpCode = GenerateTOTP.generateTOTP(totpSecretKey, userEmail)
        WebUI.clearText(inputTOTP)
        WebUI.setText(inputTOTP, totpCode)
        WebUI.delay(3)
        
        // Step 8: Click Submit TOTP
        TestObject btnSubmitTOTP = FindObjectByXPath.findObject('//lg-submit-button//button[@type="submit"]')
        WebUI.click(btnSubmitTOTP, FailureHandling.OPTIONAL)
        WebUI.delay(3)
        
        // Step 9: Grant access
        TestObject btnGrant = FindObjectByXPath.findObject('//input[@name=\'action\' and @value=\'Grant\']')
        WebUI.click(btnGrant, FailureHandling.OPTIONAL)
        WebUI.delay(3)
        
        println("✅ Login flow completed successfully")
    }
}
