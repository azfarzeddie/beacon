package com.beacon.service

import com.beacon.exception.TemplateException
import com.beacon.model.entity.Template
import com.beacon.model.request.CreateTemplateRequest
import com.beacon.model.response.CreateTemplateResponse
import com.beacon.model.response.GetTemplateResponse
import com.beacon.repository.TemplateRepository
import org.springframework.dao.DataIntegrityViolationException
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
                "Hello {{name}}",
                "welcome",
                Channel.EMAIL,
                "Welcome!"
        )

        when:
        CreateTemplateResponse response = templateService.createTemplate(request)

        then:
        0 * templateRepository.findByNotificationTypeAndChannel(*_)
        1 * templateRepository.saveAndFlush({ Template t ->
            t.channel == Channel.EMAIL &&
                    t.notificationType == "welcome" &&
                    t.body == "Hello {{name}}" &&
                    t.subject == "Welcome!"
        }) >> { Template t -> t.id = UUID.fromString("00000000-0000-0000-0000-000000000001"); t }

        response.id() == UUID.fromString("00000000-0000-0000-0000-000000000001")
        response.channel() == Channel.EMAIL
        response.notificationType() == "welcome"
    }

    def "createTemplate does not overwrite the subject when none is provided"() {
        given:
        def request = new CreateTemplateRequest("Hi", "otp", Channel.SMS, null)

        when:
        templateService.createTemplate(request)

        then:
        1 * templateRepository.saveAndFlush({ Template t -> t.subject == null }) >> { Template t -> t }
    }

    def "createTemplate maps a unique constraint violation to TemplateAlreadyExists"() {
        given:
        def request = new CreateTemplateRequest("Hi", "otp", Channel.SMS, null)

        when:
        templateService.createTemplate(request)

        then: "the database constraint is the only duplicate check"
        0 * templateRepository.findByNotificationTypeAndChannel(*_)
        1 * templateRepository.saveAndFlush(_ as Template) >> { throw new DataIntegrityViolationException("uk_channel_notification_type") }
        def e = thrown(TemplateException.TemplateAlreadyExists)
        e.message == "A template for otp and SMS already exists. Please call the PUT endpoint to update it."
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
        response.id() == id
        response.channel() == Channel.EMAIL
        response.notificationType() == "welcome"
        response.body() == "Hello {{name}}"
        response.subject() == "Welcome!"
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
        def e = thrown(IllegalArgumentException)
        e.message == "Unresolved template variables: code"
    }

    def "resolveTemplate reports every unresolved placeholder once, in order of appearance"() {
        when:
        templateService.resolveTemplate("{{b}} {{a}} {{b}} {{c}}", [c: "x"])

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "Unresolved template variables: b, a"
    }

    def "resolveTemplate treats a null variable value as unresolved"() {
        when:
        templateService.resolveTemplate("Hello {{name}}", [name: null])

        then:
        thrown(IllegalArgumentException)
    }

    def "resolveTemplate does not re-scan substituted values, so variables cannot inject each other"() {
        expect:
        templateService.resolveTemplate("Hello {{a}}", variables) == "Hello {{b}}"

        where:
        variables << [
                [a: "{{b}}", b: "INJECTED"],
                new LinkedHashMap([b: "INJECTED", a: "{{b}}"]),
                new TreeMap([a: "{{b}}", b: "INJECTED"])
        ]
    }

    def "resolveTemplate allows a legitimate value that contains braces"() {
        expect:
        templateService.resolveTemplate("Snippet: {{code}}", [code: "use {{ and }} in templates"]) ==
                "Snippet: use {{ and }} in templates"
    }

    def "resolveTemplate inserts values containing regex replacement characters literally"() {
        expect:
        templateService.resolveTemplate("Price: {{price}}", [price: 'US$5 \\ $1']) == 'Price: US$5 \\ $1'
    }

    def "resolveTemplate substitutes a repeated placeholder everywhere and ignores unused variables"() {
        expect:
        templateService.resolveTemplate("{{n}}-{{n}}", [n: "7", unused: "x"]) == "7-7"
    }
}
