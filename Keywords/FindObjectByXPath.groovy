
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.TestObjectProperty
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.ObjectRepository

/**
 * Find test object in Object Repository by XPath
 * @param xpath - XPath string
 * @return TestObject if found, null otherwise
 */
class FindObjectByXPath {
    static TestObject findObject(String xpath) {
        if (!xpath) return null
        
        // Try to find in Object Repository
        try {
            // Get all test objects from repository
            List<TestObject> allObjects = ObjectRepository.getTestObjects()
            
            for (TestObject obj : allObjects) {
                List<TestObjectProperty> props = obj.getProperties()
                for (TestObjectProperty prop : props) {
                    if (prop.getName().equalsIgnoreCase("xpath") && 
                        prop.getCondition().equals(ConditionType.EQUALS) &&
                        prop.getValue().equals(xpath)) {
                        return obj
                    }
                }
            }
        } catch (Exception e) {
            // If Object Repository lookup fails, create dynamic object
        }
        
        // If not found, create dynamic TestObject
        TestObject testObj = new TestObject()
        TestObjectProperty xpathProperty = new TestObjectProperty("xpath", ConditionType.EQUALS, xpath)
        testObj.addProperty(xpathProperty)
        return testObj
    }
}
