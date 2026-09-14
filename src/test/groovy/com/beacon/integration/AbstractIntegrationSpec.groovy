package com.beacon.integration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

/**
 * Base class for full-stack API specs: real Spring context, real Postgres
 * (via Testcontainers), through MockMvc. Each test method runs in its own
 * transaction that is rolled back afterwards, so specs never need to clean
 * up the rows they create.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = [
        "EMAIL_SERVER_HOST=localhost",
        "EMAIL_SERVER_PORT=3025",
        "EMAIL_SERVER_USERNAME=test@example.com",
        "EMAIL_SERVER_PASSWORD=test"
])
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration)
@Transactional
abstract class AbstractIntegrationSpec extends Specification {

    @Autowired
    MockMvc mockMvc
}
