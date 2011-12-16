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
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>

<link href="${pageContext.request.contextPath}/css/style.css" type="text/css" rel="stylesheet" />

<portlet:actionURL var="changePasswordAction">
    <portlet:param name="action" value="changePassword"/>
</portlet:actionURL>

<c:if test="${not empty errorMessage}">
    <div class="portlet-msg-error">
        ${errorMessage}
    </div>
</c:if>

<p>
    Lösenordet som du har till Regionportalen är samma som det du fått för att nå e-post, kalender mm via webben.
    När du ändrar ditt lösenordet kommer ändringen genomföras både för webbmailen och Regionportalen. Det kan ta
    upp till 15 minuter innan ändringen av lösenordet slår igenom.
</p>

<form id="changePasswordForm" action="${changePasswordAction}" method="POST">
    <div><b>VGR-ID:</b> ${vgrId}</div>
    <div><aui:input type="password" label="Nytt lösenord" inlineField="true" name="password"
                    helpMessage="Lösenord med både siffror och bokstäver och endast siffror och bokstäver. Minst
                    6 tecken."
                    autocomplete="off"/></div>
    <div><aui:input type="password" label="Bekräfta nytt lösenord" inlineField="true" name="passwordConfirm"
                    autocomplete="off"/></div>
    <div><input type="submit" value="Ändra lösenord"/></div>
</form>