<%--

    Copyright 2010 Västra Götalandsregionen

      This library is free software; you can redistribute it and/or modify
      it under the terms of version 2.1 of the GNU Lesser General Public
      License as published by the Free Software Foundation.

      This library is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU Lesser General Public License for more details.

      You should have received a copy of the GNU Lesser General Public
      License along with this library; if not, write to the
      Free Software Foundation, Inc., 59 Temple Place, Suite 330,
      Boston, MA 02111-1307  USA


--%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>

<link href="${pageContext.request.contextPath}/css/style.css" type="text/css" rel="stylesheet"/>

<portlet:actionURL var="changePasswordAction">
    <portlet:param name="action" value="changePassword"/>
</portlet:actionURL>

<div class="change-password-portlet">

    <c:if test="${not empty errorMessage}">
        <div class="portlet-msg-error">
                ${errorMessage}
        </div>
    </c:if>

    <p>
        Här ändrar du lösenordet som du använder för att logga in i Regionportalen.
    </p>

    <div class="user-box-container">
        <span class="user-box">Användar-id: <span class="user-id">${vgrId}</span></span>
    </div>

    <c:choose>
        <c:when test="${not empty secondsElapsed}">
            <div class="portlet-msg-info">
                Du kan byta lösenord om maximalt <span id="count-down"></span> minuter.
            </div>
        </c:when>
        <c:otherwise>
            <form class="change-password-form" action="${changePasswordAction}" method="POST">

                <div><aui:input type="password" label="Nytt lösenord" inlineField="true" name="password"
                                helpMessage="Lösenord med både siffror och bokstäver och endast siffror och bokstäver. Minst 6 tecken."
                                autocomplete="off"/></div>
                <div><aui:input type="password" label="Bekräfta nytt lösenord" inlineField="true" name="passwordConfirm"
                                autocomplete="off"/></div>
                <div><input type="submit" value="Ändra lösenord"/></div>
            </form>
        </c:otherwise>
    </c:choose>

</div>
