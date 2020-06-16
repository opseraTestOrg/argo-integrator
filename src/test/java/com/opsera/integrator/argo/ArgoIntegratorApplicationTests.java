package com.opsera.integrator.argo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ArgoIntegratorApplicationTests {
	@Test
	public void testAssertNull() {
		assertNull( null, "should be null");
	}

	@Test
	public void testAssertNotNull() {
		assertNotNull("should be not null");
	}

}
