/*
 * Copyright (c) 2015 Kagilum SAS.
 *
 * This file is part of iceScrum.
 *
 * iceScrum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * iceScrum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with iceScrum.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Authors:
 *
 * Vincent Barrier (vbarrier@kagilum.com)
 * Nicolas Noullet (nnoullet@kagilum.com)
 *
 */

package org.icescrum.web.presentation

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.util.BuildSettingsHolder
import org.icescrum.core.domain.Project
import org.icescrum.core.domain.User
import org.icescrum.core.domain.preferences.ProjectPreferences
import org.icescrum.core.error.ControllerErrorHandler
import org.icescrum.core.support.ApplicationSupport
import org.icescrum.core.utils.ServicesUtils
import org.springframework.web.servlet.support.RequestContextUtils as RCU
import sun.misc.BASE64Decoder

class ScrumOSController implements ControllerErrorHandler {

    def messageSource
    def servletContext
    def projectService
    def securityService
    def grailsApplication
    def uiDefinitionService
    def springSecurityService

    def index() {
        def user = springSecurityService.isLoggedIn() ? User.get(springSecurityService.principal.id) : null

        def context = ApplicationSupport.getCurrentContext(params)
        if (context) {
            context.indexScrumOS.delegate = this
            context.indexScrumOS(context, user, securityService, springSecurityService)
        }

        def userProjects = user ? projectService.getAllActiveProjectsByUser(user) : []
        def projectsLimit = 9
        def browsableProjectsCount = request.admin ? Project.count() : ProjectPreferences.countByHidden(false, [cache: true])

        def attrs = [user                  : user,
                     lang                  : RCU.getLocale(request).toString().substring(0, 2),
                     context               : context,
                     browsableProjectsExist: browsableProjectsCount > 0,
                     moreProjectsExist     : userProjects?.size() > projectsLimit,
                     projectFilteredsList  : userProjects.take(projectsLimit)]
        if (context) {
            attrs."$context.name" = context.object
        }
        entry.hook(id: "scrumOS-index", model: [attrs: attrs])
        attrs
    }

    def window(String windowDefinitionId) {
        if (!windowDefinitionId) {
            returnError(code: 'is.error.no.window')
            return
        }
        def windowDefinition = uiDefinitionService.getWindowDefinitionById(windowDefinitionId)
        if (windowDefinition) {
            if (!ApplicationSupport.isAllowed(windowDefinition, params)) {
                if (springSecurityService.isLoggedIn()) {
                    render(status: 403)
                } else {
                    render(status: 401, contentType: 'application/json', text: [] as JSON)
                }
                return
            }

            def context = windowDefinition.context ? ApplicationSupport.getCurrentContext(params, windowDefinition.context) : null
            def _continue = true
            if (windowDefinition.before) {
                windowDefinition.before.delegate = delegate
                windowDefinition.before.resolveStrategy = Closure.DELEGATE_FIRST
                _continue = windowDefinition.before(context?.object)
            }

            if (!_continue) {
                render(status: 404)
            } else {
                def model = [windowDefinition: windowDefinition]
                if (context) {
                    model[context.name] = context.object
                    model['contextScope'] = context.contextScope
                }
                if (ApplicationSupport.controllerExist(windowDefinition.id, "window")) {
                    forward(action: 'window', controller: windowDefinition.id, model: model)
                } else {
                    render(plugin: windowDefinition.pluginName, template: "/${windowDefinition.id}/window", model: model)
                }
            }
        } else {
            render(status: 404)
        }
    }

    def about() {
        def aboutFile = new File(grailsAttributes.getApplicationContext().getResource("/infos").getFile().toString() + File.separatorChar + "about.xml")
        render(status: 200, template: "about/index", model: [server        : servletContext.getServerInfo(),
                                                             versionNumber : g.meta([name: 'app.version']),
                                                             about         : new XmlSlurper().parse(aboutFile),
                                                             configLocation: grailsApplication.config.grails.config.locations instanceof List ? grailsApplication.config.grails.config.locations.join(', ') : ''])
    }

    def textileParser(String data) {
        render(text: ServicesUtils.textileToHtml(data))
    }

    def isSettings(Long project) {
        def projectMenus = []
        def _project = project ? Project.get(project) : null
        uiDefinitionService.getWindowDefinitions().each { windowId, windowDefinition ->
            if (windowDefinition.context == 'project') {
                projectMenus << [id: windowId, title: message(code: windowDefinition.menu?.title)]
            }
        }
        def menus = ApplicationSupport.getUserMenusContext(uiDefinitionService.getWindowDefinitions(), params)
        menus?.each {
            if (it.id == 'project') {
                it.title = _project.name
            } else {
                it.title = message(code: it.title)
            }
        }
        def defaultView = project ? menus.find { it.position == 1 && it.visible }.id ?: 'project' : 'home'
        render(status: 200,
                template: 'isSettings',
                model: [project        : _project,
                        user           : springSecurityService.currentUser,
                        roles          : securityService.getRolesRequest(false),
                        i18nMessages   : messageSource.getAllMessages(RCU.getLocale(request)),
                        resourceBundles: grailsApplication.config.icescrum.resourceBundles,
                        menus          : menus,
                        context        : ApplicationSupport.getCurrentContext(params)?.name ?: '',
                        defaultView    : defaultView,
                        serverURL      : ApplicationSupport.serverURL(),
                        projectMenus   : projectMenus])
    }

    def saveImage(String image, String title) {
        if (!image) {
            render(status: 404)
            return
        }
        title = URLDecoder.decode(title)
        image = URLDecoder.decode(image)
        image = image.substring(image.indexOf("base64,") + "base64,".length(), image.length())
        response.contentType = 'image/png'
        ['Content-disposition': "attachment;filename=\"${title + '.png'}\"", 'Cache-Control': 'private', 'Pragma': ''].each { k, v ->
            response.setHeader(k, v)
        }
        response.outputStream << new BASE64Decoder().decodeBuffer(image)
    }

    def whatsNew(boolean hide) {
        if (hide) {
            if (springSecurityService.currentUser?.preferences?.displayWhatsNew) {
                springSecurityService.currentUser.preferences.displayWhatsNew = false
            }
            render(status: 200)
            return
        }
        def dialog = g.render(template: "about/whatsNew")
        render(status: 200, contentType: 'application/json', text: [dialog: dialog] as JSON)
    }

    def version() {
        render(status: '200', text: g.meta([name: 'app.version']))
    }

    def progress() {
        if (session.progress) {
            render(status: 200, contentType: "application/json", text: session.progress as JSON)
            //we already sent the error so reset progress
            if (session.progress.error || session.progress.complete) {
                session.progress = null
            }
        } else {
            render(status: 404)
        }
    }

    def languages() {
        List locales = []
        def i18n
        if (grailsApplication.warDeployed) {
            i18n = grailsAttributes.getApplicationContext().getResource("WEB-INF/grails-app/i18n/").getFile().toString()
        } else {
            i18n = "$BuildSettingsHolder.settings.baseDir/grails-app/i18n"
        }
        //Default language
        locales << new Locale("en")
        // TODO re-enable real locale management
        //new File(i18n).eachFile {
        //    def arr = it.name.split("[_.]")
        //    if (arr[1] != 'svn' && arr[1] != 'properties' && arr[0].startsWith('messages')) {
        //        locales << (arr.length > 3 ? new Locale(arr[1], arr[2]) : arr.length > 2 ? new Locale(arr[1]) : new Locale(""))
        //    }
        //}
        locales.addAll(new Locale('en', 'US'), new Locale('fr'))
        // End TODO
        def returnLocales = locales.collectEntries { locale ->
            [(locale.toString()): locale.getDisplayName(locale).capitalize()]
        }
        render(returnLocales as JSON)
    }

    def timezones() {
        def timezones = TimeZone.availableIDs.sort().findAll {
            it.matches("^(Africa|America|Asia|Atlantic|Australia|Europe|Indian|Pacific)/.*")
        }.collectEntries {
            TimeZone timeZone = TimeZone.getTimeZone(it)
            def offset = timeZone.rawOffset
            def offsetSign = offset < 0 ? '-' : '+'
            Integer hour = Math.abs(offset / (60 * 60 * 1000))
            Integer min = Math.abs(offset / (60 * 1000)) % 60
            def calendar = Calendar.instance
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, min)
            return [(it): "$timeZone.ID (UTC$offsetSign${String.format('%tR', calendar)})"]
        }
        render(timezones as JSON)
    }

    @Secured(['isAuthenticated()'])
    def charts(String context) {
        def _charts = []
        grailsApplication.config.icescrum.contexts."$context".contextScope.charts?.each { groupName, charts ->
            if (groupName == 'project') { // TODO support charts at release / sprint levels...
                _charts.addAll(charts.collect { chart -> [id: chart.id, name: message(code: chart.name)] })
            }
        }
        render(status: 200, contentType: 'application/json', text: _charts as JSON)
    }

    @Secured(['permitAll()'])
    def apps() {
        def apps = [
                [name               : 'Management',
                 version            : '1.0',
                 installed          : false,
                 author             : 'iceScrum team',
                 publishDate        : '',
                 includedWithLicense: true,
                 website            : 'https://www.icescrum.com',
                 documentation      : 'https://www.icescrum.com/documentation/',
                 description        : 'This extension is the core of iceScrum Pro. It helps you to configure and manage your iceScrum server:' +
                         '<p>' +
                         '<ul>' +
                         '<li>Administrate your users</li>' +
                         '<li>Administrate your projects</li>' +
                         '<li>Define new administrators</li>' +
                         '<li>Update your server configuration in an nice user interface</li>' +
                         '<li>Authenticate your users through LDAP</li>' +
                         '<li>Define your product vision with the Roadmap view</li>' +
                         '<li>Define your team availabilities</li>' +
                         '<li>Export iceScrum items as custom CSV documents</li>' +
                         '<li>Switch user in the task board</li>' +
                         '<li>Create items from emails</li>' +
                         '<li>Create stories directly in the backlog, copy stories from one project to another, filter by user in the sprint plan...</li>' +
                         '</ul>' +
                         '</p>',
                 screenshots        : ['https://www.icescrum.com/wp-content/uploads/2012/06/Example-of-working-LDAP-Configuration1.jpg']
                ],
                [name         : 'Embedded',
                 version      : '1.0',
                 author       : 'iceScrum',
                 email        : 'support@kagilum.com',
                 updated      : '12/12/2016',
                 website      : 'https://www.icescrum.com',
                 documentation: 'https://www.icescrum.com/documentation/embedded',
                 description  : 'Embed live iceScrum views into your online documents or websites in order to create custom dashboards and reports.',
                 screenshots  : ['https://www.icescrum.com/wp-content/uploads/2013/01/Define-your-embedded-widgets.jpg']
                ],
                [name         : 'Icebox',
                 version      : '1.0',
                 author       : 'iceScrum',
                 email        : 'support@kagilum.com',
                 updated      : '12/12/2016',
                 website      : 'https://www.icescrum.com',
                 documentation: 'https://www.icescrum.com/documentation/icebox/',
                 description  : 'Product Owners freeze stories that don’t belong to the current product vision and restore them when the time has come.',
                 screenshots  : ['https://www.icescrum.com/wp-content/uploads/2012/11/Freeze-a-story1.jpg']
                ],
                [name         : 'Cloud Storage',
                 version      : '1.0',
                 author       : 'iceScrum',
                 email        : 'support@kagilum.com',
                 updated      : '12/12/2016',
                 website      : 'https://www.icescrum.com',
                 documentation: 'https://www.icescrum.com/documentation/cloud-storage',
                 description  : 'Attach your cloud documents in iceScrum directly from Dropbox, Google Drive or Microsoft OneDrive.',
                 screenshots  : ['https://www.icescrum.com/wp-content/uploads/2012/11/Cloud-storage-settings1.png', 'https://www.icescrum.com/wp-content/uploads/2012/11/Attachment-display1.jpg']
                ],
                [name         : 'Feedback',
                 version      : '1.0',
                 author       : 'iceScrum',
                 email        : 'support@kagilum.com',
                 updated      : '12/12/2016',
                 website      : 'https://www.icescrum.com',
                 documentation: 'https://www.icescrum.com/documentation/feedback',
                 description  : 'Include the feedback module in your website and offer your users a way to provide feedback that your collect as stories in your iceScrum project',
                 screenshots  : ['https://www.icescrum.com/wp-content/uploads/2015/06/feedback-english-650x393@2x.png', 'https://www.icescrum.com/wp-content/uploads/2015/06/story-details-2-650x424@2x.png']
                ],
                [name         : 'Team communication',
                 version      : '1.0',
                 author       : 'iceScrum',
                 email        : 'support@kagilum.com',
                 updated      : '12/12/2016',
                 website      : 'https://www.icescrum.com',
                 documentation: 'https://www.icescrum.com/documentation/team-communication',
                 description  : 'Stay informed of what happens in iceScrum by receiving important events about your stories in your Slack channel.',
                 screenshots  : ['https://www.icescrum.com/wp-content/uploads/2015/06/admin-settings-650x401@2x.png', 'https://www.icescrum.com/wp-content/uploads/2015/06/Slack1-650x461@2x.png']
                ],
                [name         : 'SCM',
                 version      : '1.0',
                 author       : 'iceScrum',
                 email        : 'support@kagilum.com',
                 updated      : '12/12/2016',
                 website      : 'https://www.icescrum.com',
                 documentation: 'https://www.icescrum.com/documentation/git-svn/',
                 description  : 'Keep track of code changes by linking your commits (from Git, GitHub, SVN…) to your tasks and user stories. Display the latest build information (status, commits, build #) from Jenkins/Hudson in iceScrum.',
                 screenshots  : ['https://www.icescrum.com/wp-content/uploads/2013/06/Build-status-on-your-project-dashboard1.jpg', 'https://www.icescrum.com/wp-content/uploads/2012/06/Enable-SCM-integration-in-your-project-settings.jpg']
                ],
                [name         : 'Bug Trackers',
                 version      : '1.0',
                 author       : 'iceScrum',
                 email        : 'support@kagilum.com',
                 updated      : '12/12/2016',
                 website      : 'https://www.icescrum.com',
                 documentation: 'https://www.icescrum.com/documentation/bug-tracker/',
                 description  : 'Automatically synchronize your project data between your bug tracker (JIRA, Mantis, Bugzilla, Redmine, TRAC…) and iceScrum.',
                 screenshots  : ['https://www.icescrum.com/wp-content/uploads/2013/08/Set-up-bug-tracker-connection.png', 'https://www.icescrum.com/wp-content/uploads/2013/08/Create-an-import-rule.-Here-every-15-min-new-issues-from-vb-filter-are-imported-as-accepted-defect-stories.png']
                ],
                [name         : 'Project Bundle',
                 version      : '1.0',
                 author       : 'iceScrum',
                 email        : 'support@kagilum.com',
                 updated      : '12/12/2016',
                 website      : 'https://www.icescrum.com',
                 documentation: 'https://www.icescrum.com/documentation/project-bundle/',
                 description  : 'Project bundles allow you to group interrelated projects, providing a big picture of their planning and progress to help you make the best decisions.',
                 screenshots  : ['https://www.icescrum.com/wp-content/uploads/2013/08/Bundle-timeline.png', 'https://www.icescrum.com/wp-content/uploads/2013/08/Total-line-for-synchronized-sprints.png']
                ],
                [name         : 'Backlogs',
                 version      : '0.1',
                 author       : 'iceScrum',
                 email        : 'support@kagilum.com',
                 updated      : '12/12/2016',
                 website      : 'https://www.icescrum.com',
                 documentation: 'https://www.icescrum.com',
                 description  : 'Create your own story backlogs according to custom filters.',
                 screenshots  : ['https://www.icescrum.com/wp-content/uploads/2016/05/extension-backlogs.png']
                ],
                [name         : 'Mood',
                 version      : '0.1',
                 author       : 'iceScrum',
                 email        : 'support@kagilum.com',
                 updated      : '12/12/2016',
                 website      : 'https://www.icescrum.com',
                 documentation: 'https://www.icescrum.com',
                 description  : 'Enter you mood everyday and help track and improve the well-being of your teams.',
                 screenshots  : ['https://www.icescrum.com/wp-content/uploads/2016/05/extension-mood.png'],
                 tags         : ["tags1", "tags3", "tags2", "tags2", "tags2", "tags2", "tags2"]
                ]
        ]
        render(status: 200, contentType: 'application/json', text: apps.sort { it.name } as JSON)
    }

    @Secured(['permitAll()'])
    def warnings() {
        def warnings = grailsApplication.config.icescrum.warnings.collect { it ->
            [id: it.id, icon: it.icon, title: message(it.title), message: message(it.message), hideable: it.hideable, silent: it.silent]
        }
        render(status: 200, contentType: 'application/json', text: warnings as JSON)
    }

    @Secured(["hasRole('ROLE_ADMIN')"])
    def hideWarning(String warningId) {
        render(status: 200, contentType: 'application/json', text: ApplicationSupport.toggleSilentWarning(warningId) as JSON)
    }

    @Secured(['permitAll()'])
    def robots() {
        render(status: 200, contentType: 'text/plain', text: 'User-agent: *\nDisallow: /')
    }
}
