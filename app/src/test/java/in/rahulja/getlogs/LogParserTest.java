package in.rahulja.getlogs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class LogParserTest {

    @Test
    public void testGetLogLineForArray_ValidJson() {
        String json = "{" +
                "\"datetime\": \"2023-10-27 10:00:00\"," +
                "\"action\": \"in.rahulja.getlogs.ACTION_TEST\"," +
                "\"data\": {" +
                "  \"key1\": \"value1\"," +
                "  \"key2\": \"[item1, item2]\"" +
                "}" +
                "}";

        String result = LogParser.getLogLineForArray(json);

        assertTrue(result.contains("2023-10-27 10:00:00"));
        assertTrue(result.contains("in.rahulja.getlogs"));
        assertTrue(result.contains("ACTION_TEST"));
        assertTrue(result.contains("key1: value1"));
        assertTrue(result.contains("key2: "));
        assertTrue(result.contains("item1"));
        assertTrue(result.contains("item2"));
    }

    @Test
    public void testGetLogLineForArray_InvalidJson() {
        String json = "{invalid_json}";
        String result = LogParser.getLogLineForArray(json);
        assertEquals("", result);
    }

    @Test
    public void testGetLogLineForArray_MissingFields() {
        String json = "{\"datetime\": \"2023-10-27\"}";
        String result = LogParser.getLogLineForArray(json);
        assertEquals("2023-10-27\n", result);
    }

    @Test
    public void testGetLogLineForArray_WifiInfo() {
         String json = "{" +
                "\"datetime\": \"2023-10-27\"," +
                "\"action\": \"test.action\"," +
                "\"data\": {" +
                "  \"wifiInfo\": \"ssid, bssid, signal\"" +
                "}" +
                "}";
        String result = LogParser.getLogLineForArray(json);
        assertTrue(result.contains("ssid"));
        assertTrue(result.contains("bssid"));
    }
}
