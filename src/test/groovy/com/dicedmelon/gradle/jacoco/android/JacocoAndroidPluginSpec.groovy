package com.dicedmelon.gradle.jacoco.android

import org.gradle.api.Project
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.tasks.JacocoReport
import spock.lang.Specification
import spock.lang.Unroll

import static com.dicedmelon.gradle.jacoco.android.AndroidProjectFactory.configureAsLibraryAndApplyPlugin
import static com.dicedmelon.gradle.jacoco.android.AndroidProjectFactory.create

class JacocoAndroidPluginSpec extends Specification {

  Project project

  def setup() {
    project = create()
    JacocoAndroidUnitTestReportExtension.defaultExcludesFactory = { ['default exclude'] }
  }

  def "should throw if android plugin not applied"() {
    when:
    project.apply plugin: JacocoAndroidPlugin

    then:
    thrown PluginApplicationException
  }

  @Unroll
  def "should apply jacoco plugin by default with #androidPlugin"() {
    expect:
    project.apply plugin: androidPlugin
    project.apply plugin: JacocoAndroidPlugin
    project.plugins.hasPlugin(JacocoPlugin)

    where:
    androidPlugin << ['com.android.library', 'com.android.application']
  }

  def "should not create jacocoTestReport task if there is one already"() {
    when:
    def jacocoTestReportTask = project.task("jacocoTestReport")
    configureAsLibraryAndApplyPlugin(project)

    then:
    project.tasks.getByName("jacocoTestReport") == jacocoTestReportTask
  }

  def "should add JacocoReport tasks for each variant"() {
    when:
    configureAsLibraryAndApplyPlugin(project)
    project.evaluate()

    then:
    project.tasks.findByName("jacocoTestPaidDebugUnitTestReport")
    project.tasks.findByName("jacocoTestFreeDebugUnitTestReport")
    project.tasks.findByName("jacocoTestPaidReleaseUnitTestReport")
    project.tasks.findByName("jacocoTestFreeReleaseUnitTestReport")
    project.tasks.findByName("jacocoTestReport")
  }

  def "should use default excludes"() {
    when:
    configureAsLibraryAndApplyPlugin(project)
    project.evaluate()

    then:
    assertAllJacocoReportTasksExclude(['default exclude', 'other'])
  }

  def "should use extension's excludes"() {
    when:
    configureAsLibraryAndApplyPlugin(project)
    project.jacocoAndroidUnitTestReport {
      excludes = ['some exclude']
    }
    project.evaluate()

    then:
    assertAllJacocoReportTasksExclude(['some exclude'])
  }

  def "should merge default and extension's excludes"() {
    when:
    configureAsLibraryAndApplyPlugin(project)
    project.jacocoAndroidUnitTestReport {
      excludes += ['some exclude']
    }
    project.evaluate()

    then:
    assertAllJacocoReportTasksExclude(['default exclude', 'some exclude'])
  }

  def assertAllJacocoReportTasksExclude(Collection<String> strings) {
    project.tasks.withType(JacocoReport).each {
      assert strings.containsAll(it.classDirectories.patternSet.excludes as Collection<String>)
    }
  }
}
