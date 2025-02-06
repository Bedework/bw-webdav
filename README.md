# bw-webdav [![Build Status](https://travis-ci.org/Bedework/bw-webdav.svg)](https://travis-ci.org/Bedework/bw-webdav)

A generic webdav server which interacts with a back end to access the
resources. Extended by the CalDAV project to provide a CalDAV server for
[Bedework](https://www.apereo.org/projects/bedework).

A functioning webdav server can be built by fully implementing the abstract
WebdavNsIntf class.

## Requirements

1. JDK 17
2. Maven 3

## Building Locally

> mvn clean install

## Releasing

Releases of this fork are published to Maven Central via Sonatype.

To create a release, you must have:

1. Permissions to publish to the `org.bedework` groupId.
2. `gpg` installed with a published key (release artifacts are signed).

To perform a new release:

> mvn release:clean release:prepare

When prompted, select the desired version; accept the defaults for scm tag and next development version.
When the build completes, and the changes are committed and pushed successfully, execute:

> mvn release:perform

For full details, see [Sonatype's documentation for using Maven to publish releases](http://central.sonatype.org/pages/apache-maven.html).

## Release Notes
### 4.0.9
* Lowercase account unless mixed case environment variable BEDEWORK_MIXEDCASE_ACCOUNTS is set to true
* Fixes to report/propfind - allprops and propname were not being handled correctly.
* Sync spec says "infinite" for depth
* Expect collection when URI ends with "/"
* Provide webdav access to WdSysIntf object. Allows prefixing of uri in error response.
* For propfind on missing resource return a 404 instead of a 207 with 404 inside.
* Return 0 length content with 200 status when no content in resource

#### 4.0.10
* Update library versions
* Minor fix to check for null. Resulted in many changes to remove throws clauses from the xml emit utility class methods. This of course resulted in many changes up the call hierarchy.

#### 4.0.11
* Update library versions

#### 4.0.12
* Update library versions

#### 5.0.0
* Use bedework-parent for builds
*  Upgrade library versions

#### 5.0.1
*  Upgrade library versions
* Add a limit to how long the sync token is valid. Will allow flushing of old tombstoned data
* Encode etag

#### 5.0.2
*  Upgrade library versions
* Make webdavexception subclass of runtimeexception and tidy up a bit. Should be no noticable changes.

#### 5.0.3
*  Upgrade library versions

#### 5.0.4
*  Upgrade library versions

#### 5.0.5
*  Upgrade library versions

#### 5.0.6
*  Upgrade library versions
* Move response classes and ToString into bw-base module.
* Pre-jakarta
