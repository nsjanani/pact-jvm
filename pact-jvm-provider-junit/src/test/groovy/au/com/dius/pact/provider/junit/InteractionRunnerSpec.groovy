package au.com.dius.pact.provider.junit

import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.FilteredPact
import au.com.dius.pact.model.Provider
import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.model.RequestResponsePact
import au.com.dius.pact.model.UnknownPactSource
import au.com.dius.pact.provider.VerificationReporter
import au.com.dius.pact.provider.junit.target.HttpTarget
import au.com.dius.pact.provider.junit.target.Target
import au.com.dius.pact.provider.junit.target.TestTarget
import org.junit.runner.notification.RunNotifier
import org.junit.runners.model.TestClass
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

class InteractionRunnerSpec extends Specification {

  @SuppressWarnings('PublicInstanceField')
  class InteractionRunnerTestClass {
    @TestTarget
    public final Target target = new HttpTarget(8332)
  }

  private clazz
  private reporter

  def setup() {
    clazz = new TestClass(InteractionRunnerTestClass)
    reporter = Mock(VerificationReporter)
  }

  def 'do not publish verification results if any interactions have been filtered'() {
    given:
    def interaction1 = new RequestResponseInteraction(description: 'Interaction 1')
    def interaction2 = new RequestResponseInteraction(description: 'Interaction 2')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1, interaction2 ])

    def filteredPact = new FilteredPact(pact, { it.description == 'Interaction 1' })
    def runner = new InteractionRunner(clazz, filteredPact, UnknownPactSource.INSTANCE)
    runner.verificationReporter = reporter
    reporter.publishingResultsDisabled() >> false

    when:
    runner.run([:] as RunNotifier)

    then:
    0 * reporter.reportResults
  }

  def 'do not publish verification results if any before step fails and publishing is not enabled'() {
    given:
    def interaction1 = new RequestResponseInteraction(description: 'Interaction 1',
      providerStates: [ new ProviderState('Test State') ])
    def interaction2 = new RequestResponseInteraction(description: 'Interaction 2')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1, interaction2 ])

    def runner = new InteractionRunner(clazz, pact, UnknownPactSource.INSTANCE)
    runner.verificationReporter = reporter
    reporter.publishingResultsDisabled() >> true

    when:
    runner.run([:] as RunNotifier)

    then:
    0 * reporter.reportResults(_, false, _, _)
  }

  def 'publish a failed verification result if any before step fails and publishing is enabled'() {
    given:
    def interaction1 = new RequestResponseInteraction(description: 'Interaction 1',
      providerStates: [ new ProviderState('Test State') ])
    def interaction2 = new RequestResponseInteraction(description: 'Interaction 2')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1, interaction2 ])

    def runner = new InteractionRunner(clazz, pact, UnknownPactSource.INSTANCE)
    runner.verificationReporter = reporter
    reporter.publishingResultsDisabled() >> false

    when:
    runner.run([:] as RunNotifier)

    then:
    1 * reporter.reportResults(_, false, _, _)
  }

  @RestoreSystemProperties
  def 'provider version trims -SNAPSHOT'() {
    given:
    System.setProperty('pact.provider.version', '1.0.0-SNAPSHOT-wn23jhd')
    def interaction1 = new RequestResponseInteraction(description: 'Interaction 1')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1 ])

    def filteredPact = new FilteredPact(pact, { it.description == 'Interaction 1' })
    def runner = new InteractionRunner(clazz, filteredPact, UnknownPactSource.INSTANCE)

    // Property true
    when:
    System.setProperty('pact.provider.version.trimSnapshot', 'true')
    def providerVersion = runner.providerVersion()

    then:
    providerVersion == '1.0.0-wn23jhd'

    // Property false
    when:
    System.setProperty('pact.provider.version.trimSnapshot', 'false')
    providerVersion = runner.providerVersion()

    then:
    providerVersion == '1.0.0-SNAPSHOT-wn23jhd'

    // Property unexpected value
    when:
    System.setProperty('pact.provider.version.trimSnapshot', 'erwf')
    providerVersion = runner.providerVersion()

    then:
    providerVersion == '1.0.0-SNAPSHOT-wn23jhd'

    // Property not present
    when:
    System.clearProperty('pact.provider.version.trimSnapshot')
    providerVersion = runner.providerVersion()

    then:
    providerVersion == '1.0.0-SNAPSHOT-wn23jhd'
  }

}
