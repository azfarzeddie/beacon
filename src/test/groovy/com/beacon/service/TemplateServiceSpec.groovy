package com.beacon.service

import com.beacon.exception.TemplateException
import com.beacon.model.entity.Template
import com.beacon.model.request.CreateTemplateRequest
import com.beacon.model.response.CreateTemplateResponse
import com.beacon.model.response.GetTemplateResponse
import com.beacon.repository.TemplateRepository
import spock.lang.Specification
import spock.lang.Subject

import static com.beacon.model.Types.Channel

class TemplateServiceSpec extends Specification {

    TemplateRepository templateRepository = Mock()

    @Subject
    TemplateService templateService = new TemplateService(templateRepository)

    def "createTemplate persists a new template and returns the created response"() {
        given:
        def request = new CreateTemplateRequest(
                templateBody: "Hello {{name}}",
                notificationType: "welcome",
                channel: Channel.EMAIL,
                subject: "Welcome!"
        )

        when:
        CreateTemplateResponse response = templateService.createTemplate(request)

        then:
        1 * templateRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL) >> Optional.empty()
        1 * templateRepository.save({ Template t ->
            t.channel == Channel.EMAIL &&
                    t.notificationType == "welcome" &&
                    t.body == "Hello {{name}}" &&
                    t.subject == "Welcome!"
        }) >> { Template t -> t.id = UUID.fromString("00000000-0000-0000-0000-000000000001"); t }

        response.id == UUID.fromString("00000000-0000-0000-0000-000000000001")
        response.channel == Channel.EMAIL
        response.notificationType == "welcome"
    }

    def "createTemplate does not overwrite the subject when none is provided"() {
        given:
        def request = new CreateTemplateRequest(templateBody: "Hi", notificationType: "otp", channel: Channel.SMS)

        when:
        templateService.createTemplate(request)

        then:
        1 * templateRepository.findByNotificationTypeAndChannel("otp", Channel.SMS) >> Optional.empty()
        1 * templateRepository.save({ Template t -> t.subject == null }) >> { Template t -> t }
    }

    def "createTemplate throws TemplateAlreadyExists when a template for the type and channel already exists"() {
        given:
        def request = new CreateTemplateRequest(templateBody: "Hi", notificationType: "otp", channel: Channel.SMS)

        when:
        templateService.createTemplate(request)

        then:
        1 * templateRepository.findByNotificationTypeAndChannel("otp", Channel.SMS) >> Optional.of(new Template())
        0 * templateRepository.save(_)
        thrown(TemplateException.TemplateAlreadyExists)
    }

    def "getTemplate returns the matching template"() {
        given:
        def id = UUID.randomUUID()
        def template = new Template(
                id: id,
                channel: Channel.EMAIL,
                notificationType: "welcome",
                body: "Hello {{name}}",
                subject: "Welcome!"
        )

        when:
        GetTemplateResponse response = templateService.getTemplate("welcome", Channel.EMAIL)

        then:
        1 * templateRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL) >> Optional.of(template)
        response.id == id
        response.channel == Channel.EMAIL
        response.notificationType == "welcome"
        response.body == "Hello {{name}}"
        response.subject == "Welcome!"
    }

    def "getTemplate throws TemplateNotFound when no template matches"() {
        when:
        templateService.getTemplate("unknown", Channel.PUSH)

        then:
        1 * templateRepository.findByNotificationTypeAndChannel("unknown", Channel.PUSH) >> Optional.empty()
        thrown(TemplateException.TemplateNotFound)
    }

    def "resolveTemplate substitutes every known placeholder"() {
        expect:
        templateService.resolveTemplate(template, variables) == expected

        where:
        template                       | variables                          || expected
        "Hello {{name}}"               | [name: "Ada"]                      || "Hello Ada"
        "{{greeting}}, {{name}}!"      | [greeting: "Hi", name: "Bea"]      || "Hi, Bea!"
        "No placeholders here"         | [:]                                || "No placeholders here"
        "{{code}} is your OTP"         | [code: "123456"]                   || "123456 is your OTP"
    }

    def "resolveTemplate throws IllegalArgumentException when a placeholder is left unresolved"() {
        when:
        templateService.resolveTemplate("Hello {{name}}, your code is {{code}}", [name: "Ada"])

        then:
        thrown(IllegalArgumentException)
    }
}
