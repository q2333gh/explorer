import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;
import org.junit.Test;

public class UUIDTest {

  @Test
  public void testUUID() {
    // Generate a random UUID and remove hyphens
    String uuid = UUID.randomUUID().toString().replaceAll("-", "");
    System.out.println(uuid);
    System.out.println(UUID.randomUUID().toString());
    // Check if the output is not null or empty
    assertNotNull(uuid);
    assertFalse(uuid.isEmpty());

    // Check if the output has 32 characters
    assertEquals(32, uuid.length());

    // Check if the output is a valid hexadecimal string
    assertTrue(uuid.matches("[0-9a-fA-F]+"));

    // Check if the output is a valid UUID without hyphens
    try {
      UUID.fromString(
          uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-"
              + uuid.substring(16, 20) + "-" + uuid.substring(20));
    } catch (IllegalArgumentException e) {
      fail("The output is not a valid UUID without hyphens");
    }
  }
}
