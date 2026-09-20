package org.springframework.samples.petclinic.testingexamples.spring;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class SpringFullTest {
    @Test
    public void springContextUpTest2(){

    }
}
