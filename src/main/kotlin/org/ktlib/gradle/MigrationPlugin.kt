package org.ktlib.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.ktlib.db.Migration
import java.io.File

class MigrationPlugin : Plugin<Project> {
    private val Project.sourceSets: SourceSetContainer
        get() =
            (this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer

    override fun apply(project: Project) {
        val name = project.gradle.startParameter.projectProperties["name"]
        val repeatable = project.gradle.startParameter.projectProperties["repeatable"] == "true"
        val baseline = project.gradle.startParameter.projectProperties["baseline"] == "true"
        val undo = project.gradle.startParameter.projectProperties["undo"]
        val environment = project.gradle.startParameter.projectProperties["environment"]
        val rootDir = File(project.projectDir, "src/main")

        project.project.tasks.register("createSqlMigration") {
            it.group = "database"
            it.doLast {
                Migration.create(rootDir, name, false, repeatable, baseline, undo)
            }
        }

        project.tasks.register("createKotlinMigration") {
            it.group = "database"
            it.doLast {
                Migration.create(rootDir, name, true, repeatable, baseline, undo)
            }
        }

        project.tasks.register("migrate", JavaExec::class.java) {
            it.dependsOn("classes")
            it.group = "database"
            it.mainClass.set("org.ktlib.db.MigrationKt")
            if (environment != null)
                it.systemProperties = mapOf("environment" to environment)
            it.classpath = it.project.sourceSets.asMap["main"]!!.runtimeClasspath
        }
    }
}