/*
 * Copyright (c) 2014 Kagilum.
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
 */

class ProjectUrlMappings {

    static mappings = {

        "/p/$project/search" {
            controller = 'search'
            action = 'index'
            constraints {
                project(matches: /[0-9A-Z]*/)
            }
        }
        name baseUrlProject: "/p/$project/" {
            controller = 'scrumOS'
            action = 'index'
            constraints {
                project(matches: /[0-9A-Z]*/)
            }
        }
        // Scrum OS & generic
        "/p/$project/ui/window/$windowDefinitionId" {
            controller = 'scrumOS'
            action = 'window'
            constraints {
                windowDefinitionId(matches: /[a-zA-Z]*/)
                project(matches: /[0-9A-Z]*/)
            }
        }
        "/p/$project/$controller/$id?" {
            constraints {
                id(matches: /\d*/)
                project(matches: /[0-9A-Z]*/)
            }
        }
        "/p/$project/$controller/$id?/$action?/$subid?" {
            constraints {
                project(matches: /[0-9A-Z]*/)
                id(matches: /\d*/)
                subid(matches: /\d*/)
            }
        }
        name urlProject: "/p/$project/$controller/$action?/$id?/$type?" {
            constraints {
                project(matches: /[0-9A-Z]*/)
            }
        }
        // new way to handle requests (REST Style)
        "/p/$project/$controller/$id/print/$format?" {
            action = 'print'
            constraints {
                id(matches: /\d*/)
                project(matches: /[0-9A-Z]*/)
                format(matches: /[0-9A-Z]*/)
            }
        }
        "/p/$project/$controller/print/$format?" {
            action = 'print'
            constraints {
                project(matches: /[0-9A-Z]*/)
                format(matches: /[0-9A-Z]*/)
            }
        }
        // Task
        "/p/$project/task" {
            controller = 'task'
            action = [POST:"save"]
            constraints {
                project(matches: /[0-9A-Z]*/)
            }
        }
        "/p/$project/task/$id" {
            controller = 'task'
            action = [GET: "show", PUT:"update", DELETE:'delete', POST:'update']
            constraints {
                project(matches: /[0-9A-Z]*/)
                id(matches: /\d*/)
            }
        }
        "/p/$project/task/$id/$action" {
            controller = 'task'
            constraints {
                project(matches: /[0-9A-Z]*/)
                id(matches: /\d*/)
            }
        }
        "/p/$project/task/$type/$id" {
            controller = 'task'
            action = [GET: "index"]
            constraints {
                project(matches: /[0-9A-Z]*/)
                id(matches: /\d*/)
                type(inList: ['story', 'sprint'])
            }
        }
        "/p/$project/task/colors" {
            controller = 'task'
            action = [GET: "colors"]
            constraints {
                project(matches: /[0-9A-Z]*/)
            }
        }
        // Story
        "/p/$project/story" {
            controller = 'story'
            action = [GET: "index", POST:"save"]
            constraints {
                project(matches: /[0-9A-Z]*/)
            }
        }
        "/p/$project/story/$type/$typeId" {
            controller = 'story'
            action = 'index'
            constraints {
                project(matches: /[0-9A-Z]*/)
                typeId(matches: /\d*/)
                type(inList: ['actor', 'feature', 'sprint', 'backlog'])
            }
        }
        "/p/$project/story/$id" {
            controller = 'story'
            action = [GET: "show", PUT:"update", DELETE:'delete', POST:'update']
            constraints {
                project(matches: /[0-9A-Z]*/)
                id(matches: /\d+(,\d+)*/)
            }
        }
        "/p/$project/story/$id/$action" {
            controller = 'story'
            constraints {
                project(matches: /[0-9A-Z]*/)
                id(matches: /\d+(,\d+)*/)
            }
        }
        "/p/$project/story/listByField" {
            controller = 'story'
            action = 'listByField'
            constraints {
                project(matches: /[0-9A-Z]*/)
            }
        }
        // Actor
        "/p/$project/actor" {
            controller = 'actor'
            action = [GET: 'index', POST: 'save']
            constraints {
                project(matches: /[0-9A-Z]*/)
            }
        }
        "/p/$project/actor/$id" {
            controller = 'actor'
            action = [GET: 'show', PUT: 'update',  POST: 'update', DELETE: 'delete']
            constraints {
                project(matches: /[0-9A-Z]*/)
                id(matches: /\d+(,\d+)*/)
            }
        }
        // Feature
        "/p/$project/feature" {
            controller = 'feature'
            action = [GET: "index", POST:"save"]
            constraints {
                project(matches: /[0-9A-Z]*/)
            }
        }
        "/p/$project/feature/$id" {
            controller = 'feature'
            action = [GET: "show", PUT:"update", DELETE:'delete', POST:'update']
            constraints {
                project(matches: /[0-9A-Z]*/)
                id(matches: /\d+(,\d+)*/)
            }
        }
        "/p/$project/feature/$id/$action" {
            controller = 'feature'
            constraints {
                project(matches: /[0-9A-Z]*/)
                id(matches: /\d+(,\d+)*/)
            }
        }
        // Activity
        "/p/$project/activity/$type/$fluxiableId" {
            controller = 'activity'
            action = [GET: "index"]
            constraints {
                project(matches: /[0-9A-Z]*/)
                type(inList: ['story', 'task'])
                fluxiableId(matches: /\d*/)
            }
        }
        // Comment
        "/p/$project/comment/$type/$commentable" {
            controller = 'comment'
            action = [GET: "index", POST:"save"]
            constraints {
                project(matches: /[0-9A-Z]*/)
                type(inList: ['story', 'task'])
                commentable(matches: /\d*/)
            }
        }
        "/p/$project/comment/$type/$commentable/$id" {
            controller = 'comment'
            action = [GET: "show", PUT:"update", DELETE:"delete", POST:'update']
            constraints {
                project(matches: /[0-9A-Z]*/)
                type(inList: ['story', 'task'])
                id(matches: /\d*/)
                commentable(matches: /\d*/)
            }
        }
        // Acceptance test
        "/p/$project/acceptanceTest/story/$parentStory" {
            controller = 'acceptanceTest'
            action = [GET: "index"]
            constraints {
                project(matches: /[0-9A-Z]*/)
                parentStory(matches: /\d*/)
            }
        }
        "/p/$project/acceptanceTest" {
            controller = 'acceptanceTest'
            action = [POST:"save"]
            constraints {
                project(matches: /[0-9A-Z]*/)
            }
        }
        "/p/$project/acceptanceTest/$id" {
            controller = 'acceptanceTest'
            action = [POST:"update", DELETE:"delete"]
            constraints {
                id(matches: /\d*/)
                project(matches: /[0-9A-Z]*/)
            }
        }
        // Backlog
        "/p/$project/backlog" {
            controller = 'backlog'
            action = [GET: "index"]
            constraints {
                project(matches: /[0-9A-Z]*/)
            }
        }
        "/p/$project/backlog/$id/printPostits/$format?" {
            controller = 'backlog'
            action = 'printPostits'
            constraints {
                id(matches: /\d*/)
                project(matches: /[0-9A-Z]*/)
                format(matches: /[0-9A-Z]*/)
            }
        }
        // Release
        "/p/$project/release" {
            controller = 'release'
            action = [GET: "index", POST: "save"]
            constraints {
                project(matches: /[0-9A-Z]*/)
            }
        }
        "/p/$project/release/$id" {
            controller = 'release'
            action = [GET: 'show', PUT: 'update', POST: 'update', DELETE: 'delete']
            constraints {
                project(matches: /[0-9A-Z]*/)
                id(matches: /\d+(,\d+)*/)
            }
        }
        "/p/$project/release/$id/$action" {
            controller = 'release'
            constraints {
                project(matches: /[0-9A-Z]*/)
                id(matches: /\d+(,\d+)*/)
            }
        }
        // Sprint
        "/p/$project/sprint" {
            controller = 'sprint'
            action = [POST: "save"]
            constraints {
                project(matches: /[0-9A-Z]*/)
            }
        }
        "/p/$project/sprint/$id" {
            controller = 'sprint'
            action = [GET: 'show', PUT: 'update', POST: 'update', DELETE: 'delete']
            constraints {
                project(matches: /[0-9A-Z]*/)
                id(matches: /\d+(,\d+)*/)
            }
        }
        "/p/$project/sprint/$id/$action" {
            controller = 'sprint'
            constraints {
                project(matches: /[0-9A-Z]*/)
                id(matches: /\d+(,\d+)*/)
            }
        }
        "/p/$project/sprint/release/$releaseId" {
            controller = 'sprint'
            action = 'index'
            constraints {
                project(matches: /[0-9A-Z]*/)
                releaseId(matches: /[0-9A-Z]*/)
            }
        }
        "/p/$project/sprint/release/$releaseId/generateSprints" {
            controller = 'sprint'
            action = 'generateSprints'
            constraints {
                project(matches: /[0-9A-Z]*/)
                releaseId(matches: /[0-9A-Z]*/)
            }
        }
    }
}
