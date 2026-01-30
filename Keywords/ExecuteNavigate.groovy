
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/**
 * Execute navigate action
 * @param url - URL to navigate to
 * @param waitTime - Optional wait time in ms before navigation
 */
class ExecuteNavigate {
    static void navigate(String url, Integer waitTime = null) {
        if (!url) {
            throw new Exception("URL is required for navigate action")
        }
        
        if (waitTime != null && waitTime > 0) {
            WebUI.delay(waitTime.intValue() / 1000)
        }
        
        WebUI.navigateToUrl(url)
    }
}
