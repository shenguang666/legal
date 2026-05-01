package com.legal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "elasticsearch.worker.enabled=false",
        "legal.memory.worker.enabled=false",
        "legal.contract-review.worker.enabled=false"
})
@ActiveProfiles("test")
class LegalApplicationTests {

    @Test
    void contextLoads() {
    }

}
