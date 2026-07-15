package com.synergen.vobworkbench;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(TestDatabaseCleanupExtension.class)
class VobWorkbenchApplicationTests {

	@Test
	void contextLoads() {
	}

}
