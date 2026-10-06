package com.beacon.integration

import com.beacon.model.entity.Template
import com.beacon.repository.TemplateRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException

import static com.beacon.model.Types.Channel

class TemplateRepositorySpec extends AbstractIntegrationSpec {

    @Autowired
    TemplateRepository templateRepository

    private static Template template(String type, Channel channel) {
        new Template(notificationType: type, channel: channel, body: "Hello")
    }

    def "the database rejects a second template for the same notificationType and channel"() {
        given:
        templateRepository.saveAndFlush(template("welcome", Channel.EMAIL))

        when:
        templateRepository.saveAndFlush(template("welcome", Channel.EMAIL))

        then:
        thrown(DataIntegrityViolationException)
    }

    def "the same notificationType is allowed on different channels"() {
        when:
        templateRepository.saveAndFlush(template("welcome", Channel.EMAIL))
        templateRepository.saveAndFlush(template("welcome", Channel.SMS))

        then:
        noExceptionThrown()
        templateRepository.findByNotificationTypeAndChannel("welcome", Channel.SMS).isPresent()
    }
}
