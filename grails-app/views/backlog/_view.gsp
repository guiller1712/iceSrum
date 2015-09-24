<%@ page import="org.icescrum.core.domain.Story; grails.converters.JSON" %>
%{--
- Copyright (c) 2014 Kagilum SAS.
-
- This file is part of iceScrum.
-
- iceScrum is free software: you can redistribute it and/or modify
- it under the terms of the GNU Affero General Public License as published by
- the Free Software Foundation, either version 3 of the License.
-
- iceScrum is distributed in the hope that it will be useful,
- but WITHOUT ANY WARRANTY; without even the implied warranty of
- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
- GNU General Public License for more details.
-
- You should have received a copy of the GNU Affero General Public License
- along with iceScrum.  If not, see <http://www.gnu.org/licenses/>.
-
- Authors:
-
- Vincent Barrier (vbarrier@kagilum.com)
- Nicolas Noullet (nnoullet@kagilum.com)
--}%
<div id="backlog-layout-window-${controllerName}"
     %{--ui-selectable="selectableOptions"--}%
     %{--ui-selectable-list="stories"--}%
     html-sortable="storySortableOptions"
     html-sortable-callback="storySortableUpdate(startModel, destModel, start, end)"
     ng-model="filteredAndSortedStories"
     ng-class="view.asList ? 'list-group' : 'grid-group'"
     class="postits"
     ng-include="'story.html'"
     ng-init=""></div>
<script>
    angular.element(document).injector().get('StoryService').addStories(${stories as JSON});
</script>