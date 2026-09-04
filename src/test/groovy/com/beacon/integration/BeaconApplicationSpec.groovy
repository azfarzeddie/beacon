package com.beacon.integration

class BeaconApplicationSpec extends AbstractIntegrationSpec {

    def "the application context loads against a real Postgres instance"() {
        expect:
        mockMvc != null
    }
}
