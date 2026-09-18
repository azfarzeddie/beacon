package com.beacon.repository

import com.beacon.integration.AbstractIntegrationSpec
import com.beacon.model.MessageDetails
import com.beacon.model.entity.BulkNotificationJob
import com.beacon.model.entity.NotificationActionRecord
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired

import static com.beacon.model.Types.*

class NotificationActionRecordRepositorySpec extends AbstractIntegrationSpec {

    @Autowired
    BulkNotificationJobRepository jobRepository

    @Autowired
    NotificationActionRecordRepository actionRepository

    @Autowired
    EntityManager entityManager

    def "persists a job with its summary and actions with JSONB details"() {
        given:
        def job = new BulkNotificationJob(actionCount: 2, jobSummary: [USER_NOT_FOUND: 1])
        job = jobRepository.save(job)

        def pushAction = new NotificationActionRecord(
                job: job, userExternalId: "u-1", notificationType: "welcome", channel: Channel.PUSH,
                status: ActionStatus.SUCCESS,
                messageDetails: new MessageDetails("Hello", null),
                deviceTokens: ["token-a", "token-b"])
        def failedAction = new NotificationActionRecord(
                job: job, userExternalId: "u-2", notificationType: "welcome", channel: Channel.EMAIL,
                status: ActionStatus.FAILED, failureReason: "No user with externalId u-2 exists.")
        actionRepository.saveAll([pushAction, failedAction])
        entityManager.flush()
        entityManager.clear()

        when:
        def reloadedJob = jobRepository.findById(job.id).get()
        def actions = actionRepository.findByJobId(job.id)

        then:
        reloadedJob.status == JobStatus.PENDING
        reloadedJob.jobSummary == [USER_NOT_FOUND: 1]
        reloadedJob.createdAt != null
        actions.size() == 2
        def push = actions.find { it.channel == Channel.PUSH }
        push.messageDetails == new MessageDetails("Hello", null)
        push.deviceTokens == ["token-a", "token-b"]
        actions.find { it.channel == Channel.EMAIL }.messageDetails == null
    }

    def "persists an action without a job for single-send notifications"() {
        when:
        def saved = actionRepository.saveAndFlush(new NotificationActionRecord(
                userExternalId: "u-3", notificationType: "otp", channel: Channel.SMS,
                status: ActionStatus.SKIPPED))

        then:
        saved.id != null
        saved.job == null
        actionRepository.findByUserExternalId("u-3")*.id == [saved.id]
    }

    def "counts actions of a job grouped by status"() {
        given:
        def job = jobRepository.save(new BulkNotificationJob(actionCount: 4))
        def otherJob = jobRepository.save(new BulkNotificationJob(actionCount: 1))
        [ActionStatus.SUCCESS, ActionStatus.SUCCESS, ActionStatus.FAILED, ActionStatus.SKIPPED].each {
            actionRepository.save(new NotificationActionRecord(job: job, userExternalId: "u", notificationType: "t",
                    channel: Channel.EMAIL, status: it))
        }
        actionRepository.save(new NotificationActionRecord(job: otherJob, userExternalId: "u", notificationType: "t",
                channel: Channel.EMAIL, status: ActionStatus.FAILED))

        when:
        def counts = actionRepository.countByStatusForJob(job.id).collectEntries { [it.status, it.count] }

        then:
        counts == [(ActionStatus.SUCCESS): 2L, (ActionStatus.FAILED): 1L, (ActionStatus.SKIPPED): 1L]
    }
}
