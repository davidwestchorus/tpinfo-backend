#!/usr/bin/env kscript
/**
 * Copyright (C) 2013-2020 Lars Erik Röjerås
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import java.io.File
import java.nio.file.DirectoryIteratorException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import kotlin.system.exitProcess
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset


//INCLUDE ./LeoLib.kts

// -------------------------------------------------------------------------------------------
// Main program
// -------------------------------------------------------------------------------------------
/**
 *
 */

Largument.initialise(
    """
    This script builds a hippo backend in a Docker image
    It must be run from the base dir in the git project
    """.trimIndent()
)

// Largument("clean", "Do a gradle clean before the build", false)
// Largument("environment", "Specify 'qa' | 'prod'", true, "environment")
Largument("push", "Push image to NoGui docker registry", false)
Largument("run", "Run the docker image", false)
Largument("help", "Show this help information", false)

Largument.parse(args)

if (Largument.isSet("help") || (! (Largument.isSet("run") || (Largument.isSet("push"))))) Largument.showUsageAndExit("One of '--run' or '--push' must be specified!")

val PORT = 7777

// -------------------------------------------------------------------------------------------

// Verify that the current branch is committed
val statusMsg: String = lExec("git status -s", quiet = true) as String
val isCommitted = statusMsg.isEmpty()
if (! isCommitted) {
    println("The current branch is not committed! No image will be built.")
    exitProcess(1)
}

// Verify that the current commit is tagged
val gitDescribe: String = lExec("git describe", quiet = false) as String
val isTagged = ! gitDescribe.contains("-")
if (! isTagged && Largument.isSet("push")) {
    println("The current commit is not tagged! The image will not be build nor pushed.")
    exitProcess(1)
}

// Get branch name and commit hash
val gitBranch = lExec("git rev-parse --abbrev-ref HEAD", quiet = true)
val gitHash = lExec("git rev-parse --short HEAD", quiet = true) // Short version. Long can be reconstructed with the rev-parse command.

//val minutesSinceEpoch = minutesSinceEpoch()
//val dockerBuildId = "$minutesSinceEpoch-$gitHash"

val imageBaseTag = "tpinfo-backend:$gitDescribe"
val localImageTag = "rojeras/$imageBaseTag"
val noguiImageTag = "docker-registry.centrera.se:443/sll-tpinfo/$imageBaseTag"

val buildDirName = "src/"
// val buildName = "hippo-1.0.0-SNAPSHOT"
// val zipDirName = "$buildDirName/$buildName"
// val buildZipFile = "$buildDirName/$buildName.zip"
// val indexHtmlFile = "$zipDirName/index.html"

val currentDir = lPwd()

val dateTime = LocalDateTime.now()

val versionInfo = """
{
"version" : "$gitDescribe",
"createDate" : "$dateTime",
"branch" : "$gitBranch",
"hash" : "$gitHash"
}""".trimIndent()

File("src/versionInfo.json").writeText(versionInfo)
// -------------------------------------------------------------------------------------------
// If the image is to be pushed (QA/Production) then we should always do a "git clean"

/*
if (Largument.isSet("clean") || Largument.isSet("push")) lExec("./gradlew clean")

lExec("./gradlew zip")
File(zipDirName).walkBottomUp().forEach {
    lExec("rm $it", quiet = true)
}

println("Unzip")
lExec("unzip -d $zipDirName $buildZipFile", quiet = true)
*/
// Append version info as comment at end of index.html
//File(indexHtmlFile).appendText(versionInfo)

// Edit index.html and include correct version info (stored in gitDescribe variable)
/*
val file = File(indexHtmlFile)
val tempFile = createTempFile()
val regex = Regex("""<meta id="hippoVersion" content="0.0.0">""")
tempFile.printWriter().use { writer ->
    file.forEachLine { line ->
        println(line)
        writer.println(when {
            regex.matches(line.trim()) -> """<meta id="hippoVersion" content="$gitDescribe">"""
            else -> line
        })
    }
    writer.print(versionInfo)
}
check(file.delete() && tempFile.renameTo(file)) { "failed to replace file" }
 */
// ------------------------------------------------------------------------------

lExec("rm src/cache/*")

lExec("docker build --rm -t $localImageTag .", quiet = false)

if (Largument.isSet("push")) {
        lExec("docker tag $localImageTag $noguiImageTag")
        lExec("docker push $noguiImageTag")
}

if (Largument.isSet("run")) {
    lExec("bin/docker-run-backend.sh $localImageTag $PORT")
    println("The image is running and listen to port $PORT")
}

exitProcess(0)
// ---------------------------------------------------------------------------------------------
