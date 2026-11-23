package com.pulse.PulsePass;

import com.pulse.PulsePass.support.PostgresContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresContainerConfig.class)
class PulsePassApplicationTests {

	@Test
	void contextLoads() {
	}

}
